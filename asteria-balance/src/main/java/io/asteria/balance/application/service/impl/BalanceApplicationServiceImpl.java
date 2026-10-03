package io.asteria.balance.application.service.impl;

import io.asteria.balance.application.command.*;
import io.asteria.balance.application.service.BalanceApplicationService;
import io.asteria.balance.domain.entity.Balance;
import io.asteria.balance.domain.entity.BalanceMovement;
import io.asteria.balance.domain.entity.BalanceReservation;
import io.asteria.balance.domain.enums.BalanceMovementType;
import io.asteria.balance.domain.enums.BalanceReservationStatus;
import io.asteria.balance.domain.error.BalanceErrorCode;
import io.asteria.balance.domain.exception.BalanceDomainException;
import io.asteria.balance.domain.repository.BalanceMovementRepository;
import io.asteria.balance.domain.repository.BalanceRepository;
import io.asteria.balance.domain.repository.BalanceReservationRepository;
import io.asteria.balance.domain.valueobject.BalanceId;
import io.asteria.balance.domain.valueobject.BalanceMovementId;
import io.asteria.balance.domain.valueobject.BalanceReservationId;
import io.asteria.common.application.port.DistributedIdGenerator;
import io.asteria.common.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 余额功能接口定义实现类
 * */
@Slf4j
@Service
@RequiredArgsConstructor
public class BalanceApplicationServiceImpl implements BalanceApplicationService {

    private final BalanceRepository balanceRepository;
    private final BalanceReservationRepository balanceReservationRepository;
    private final BalanceMovementRepository balanceMovementRepository;
    private final DistributedIdGenerator distributedIdGenerator;

    /**
     * 增加可用余额。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void credit(CreditBalanceCommand command) {

        // 查 Balance
        Optional<Balance> oldBalanceOptional = balanceRepository.findByBalanceAccountIdAndCurrency(
                command.getBalanceAccountId(), command.getCurrency()
        );
        if (oldBalanceOptional.isEmpty()) {
            log.warn("Balance doesn't exist: {}, {}", command.getBalanceAccountId(), command.getCurrency());
            throw new BalanceDomainException(BalanceErrorCode.INVALID_PARAMS);
        }
        // oldBalance.credit(amount)
        Balance oldBalance = oldBalanceOptional.get();
        // 幂等校验
        boolean isMovementExists = balanceMovementRepository.existsByEventIdAndBalanceIdAndMovementType(
                command.getEventId(), oldBalance.getBalanceId(), BalanceMovementType.CREDIT
        );
        if (isMovementExists) {
            log.warn("Credit movement already exists: {}, {}",
                    command.getEventId(), oldBalance.getBalanceId().value());
            return;
        }

        oldBalance.credit(command.getAmount());

        // 创建 CREDIT Movement
        BalanceMovement newBalanceMovement = BalanceMovement.create(
                BalanceMovementId.of(distributedIdGenerator.nextId()),
                oldBalance.getBalanceId(),
                BalanceMovementType.CREDIT,
                command.getAmount(),
                command.getReferenceType(),
                command.getReferenceId(),
                command.getEventId()
        );
        // update Balance
        balanceRepository.update(oldBalance);
        // save Movement
        balanceMovementRepository.save(newBalanceMovement);
    }

    /**
     * 预留余额：将可用余额转入预留余额。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reserve(ReserveBalanceCommand command) {

        // 查询账户
        Optional<Balance> oldBalanceOptional = balanceRepository.findByBalanceAccountIdAndCurrency(
                command.getBalanceAccountId(), command.getCurrency()
        );
        if (oldBalanceOptional.isEmpty()) {
            log.warn("Balance doesn't exist: {}, {}", command.getBalanceAccountId(), command.getCurrency());
            throw new BalanceDomainException(BalanceErrorCode.INVALID_PARAMS);
        }
        Balance oldBalance = oldBalanceOptional.get();

        // 幂等校验
        boolean isMovementExists = balanceMovementRepository.existsByEventIdAndBalanceIdAndMovementType(
                command.getEventId(), oldBalance.getBalanceId(), BalanceMovementType.RESERVE
        );
        if (isMovementExists) {
            log.warn("Reserve movement already exists: {}, {}",
                    command.getEventId(), oldBalance.getBalanceId().value());
            return;
        }

        // 查询预留记录
        Optional<BalanceReservation> reservationOptional = balanceReservationRepository.findByBalanceIdAndReference(
                oldBalance.getBalanceId(), command.getReferenceType(), command.getReferenceId()
        );
        if (reservationOptional.isPresent()) {
            log.warn("Reservation already exists: {}, {}, {}",
                    oldBalance.getBalanceId().value(), command.getReferenceType(), command.getReferenceId());
            BalanceReservation reservation = reservationOptional.get();
            boolean isAmountEquals = reservation.getAmount().amount()
                    .compareTo(command.getAmount().amount()) == 0;
            boolean isCurrencyEquals = reservation.getAmount().currency().value()
                    .equalsIgnoreCase(command.getAmount().currency().value());
            if (!isAmountEquals || !isCurrencyEquals) {
                log.warn("Reservation is conflict: {}, {}",
                        JsonUtils.toJson(reservation), JsonUtils.toJson(command));
                throw new BalanceDomainException(BalanceErrorCode.RESERVATION_REQUEST_CONFLICT);
            }

            if (reservation.getStatus() != BalanceReservationStatus.RESERVED) {
                log.warn("Reservation status isn't reserved, {}, {}",
                        reservation.getReservationId().value(), reservation.getStatus());
                throw new BalanceDomainException(BalanceErrorCode.RESERVATION_STATUS_INCORRECT);
            }
            return;
        }

        // 余额记录存在，进行预留
        oldBalance.reserve(command.getAmount());

        // 创建预留记录
        BalanceReservation newBalanceReservation = BalanceReservation.create(
                BalanceReservationId.of(distributedIdGenerator.nextId()),
                oldBalance.getBalanceId(),
                command.getAmount(),
                command.getReferenceType(),
                command.getReferenceId()
        );

        // 创建余额变动流水
        BalanceMovement newBalanceMovement = BalanceMovement.create(
                BalanceMovementId.of(distributedIdGenerator.nextId()),
                oldBalance.getBalanceId(),
                BalanceMovementType.RESERVE,
                command.getAmount(),
                command.getReferenceType(),
                command.getReferenceId(),
                command.getEventId()
        );

        // 操作数据表
        balanceReservationRepository.save(newBalanceReservation);
        balanceRepository.update(oldBalance);
        balanceMovementRepository.save(newBalanceMovement);
    }

    /**
     * 释放预留余额：将预留余额退回可用余额。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void release(ReleaseBalanceCommand command) {

        // 查询账户
        Optional<Balance> oldBalanceOptional = balanceRepository.findByBalanceAccountIdAndCurrency(
                command.getBalanceAccountId(), command.getCurrency()
        );
        if (oldBalanceOptional.isEmpty()) {
            log.warn("Balance doesn't exist: {}, {}", command.getBalanceAccountId(), command.getCurrency());
            throw new BalanceDomainException(BalanceErrorCode.INVALID_PARAMS);
        }
        Balance oldBalance = oldBalanceOptional.get();

        // 幂等校验
        boolean isMovementExists = balanceMovementRepository.existsByEventIdAndBalanceIdAndMovementType(
                command.getEventId(), oldBalance.getBalanceId(), BalanceMovementType.RELEASE
        );
        if (isMovementExists) {
            log.warn("Release movement already exists: {}, {}",
                    command.getEventId(), oldBalance.getBalanceId().value());
            return;
        }

        // 查询预留记录
        Optional<BalanceReservation> reservationOptional = balanceReservationRepository.findByBalanceIdAndReference(
                oldBalance.getBalanceId(), command.getReferenceType(), command.getReferenceId()
        );
        if (reservationOptional.isEmpty()) {
            log.warn("Reservation doesn't exist: {}, {}, {}",
                    oldBalance.getBalanceId().value(), command.getReferenceType(), command.getReferenceId());
            throw new BalanceDomainException(BalanceErrorCode.INVALID_PARAMS);
        }

        // 释放占用余额
        BalanceReservation oldReservation = reservationOptional.get();
        oldReservation.release();
        oldBalance.release(oldReservation.getAmount());

        // 创建余额变动流水
        BalanceMovement newBalanceMovement = BalanceMovement.create(
                BalanceMovementId.of(distributedIdGenerator.nextId()),
                oldBalance.getBalanceId(),
                BalanceMovementType.RELEASE,
                oldReservation.getAmount(),
                command.getReferenceType(),
                command.getReferenceId(),
                command.getEventId()
        );

        // 操作数据表
        balanceMovementRepository.save(newBalanceMovement);
        balanceReservationRepository.update(oldReservation);
        balanceRepository.update(oldBalance);
    }

    /**
     * 扣除预留余额：将预留金额正式从余额中扣除。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void consume(ConsumeBalanceCommand command) {

        // 查询账户
        Optional<Balance> oldBalanceOptional = balanceRepository.findByBalanceAccountIdAndCurrency(
                command.getBalanceAccountId(), command.getCurrency()
        );
        if (oldBalanceOptional.isEmpty()) {
            log.warn("Balance doesn't exist: {}, {}", command.getBalanceAccountId(), command.getCurrency());
            throw new BalanceDomainException(BalanceErrorCode.INVALID_PARAMS);
        }
        Balance oldBalance = oldBalanceOptional.get();

        // 幂等校验
        boolean isMovementExists = balanceMovementRepository.existsByEventIdAndBalanceIdAndMovementType(
                command.getEventId(), oldBalance.getBalanceId(), BalanceMovementType.DEBIT_RESERVED
        );
        if (isMovementExists) {
            log.warn("Consume movement already exists: {}, {}",
                    command.getEventId(), oldBalance.getBalanceId().value());
            return;
        }

        // 查询预留记录
        Optional<BalanceReservation> reservationOptional = balanceReservationRepository.findByBalanceIdAndReference(
                oldBalance.getBalanceId(), command.getReferenceType(), command.getReferenceId()
        );
        if (reservationOptional.isEmpty()) {
            log.warn("Reservation doesn't exist: {}, {}, {}",
                    oldBalance.getBalanceId().value(), command.getReferenceType(), command.getReferenceId());
            throw new BalanceDomainException(BalanceErrorCode.INVALID_PARAMS);
        }

        // 消费预留金额
        BalanceReservation oldReservation = reservationOptional.get();
        oldReservation.consume();

        // 扣减预留金额
        oldBalance.debitReserved(oldReservation.getAmount());

        // 创建余额变动流水
        BalanceMovement newBalanceMovement = BalanceMovement.create(
                BalanceMovementId.of(distributedIdGenerator.nextId()),
                oldBalance.getBalanceId(),
                BalanceMovementType.DEBIT_RESERVED,
                oldReservation.getAmount(),
                command.getReferenceType(),
                command.getReferenceId(),
                command.getEventId()
        );

        // 操作数据表
        balanceMovementRepository.save(newBalanceMovement);
        balanceReservationRepository.update(oldReservation);
        balanceRepository.update(oldBalance);
    }

    /**
     * 开户
     */
    @Override
    public void create(CreateBalanceCommand command) {

        Optional<Balance> oldBalanceOptional = balanceRepository.findByBalanceAccountIdAndCurrency(
                command.getBalanceAccountId(), command.getCurrency()
        );
        if (oldBalanceOptional.isPresent()) {
            log.warn("Balance already exists: {}, {}", command.getBalanceAccountId().value(), command.getCurrency());
            throw new BalanceDomainException(BalanceErrorCode.BALANCE_ALREADY_EXISTS);
        }
        Balance newBalance = Balance.create(
                BalanceId.of(distributedIdGenerator.nextId()),
                command.getBalanceAccountId(),
                command.getCurrency()
        );
        balanceRepository.save(newBalance);
        log.info("Balance create successfully: {}, {}",
                command.getBalanceAccountId().value(), command.getCurrency());
    }
}
