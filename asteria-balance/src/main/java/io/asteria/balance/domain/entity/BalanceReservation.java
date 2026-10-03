package io.asteria.balance.domain.entity;

import io.asteria.balance.domain.enums.BalanceReservationStatus;
import io.asteria.balance.domain.error.BalanceErrorCode;
import io.asteria.balance.domain.exception.BalanceDomainException;
import io.asteria.balance.domain.valueobject.BalanceId;
import io.asteria.balance.domain.valueobject.BalanceReservationId;
import io.asteria.common.domain.valueobject.Money;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

/**
 * 余额预留；仅生效中的预留可释放或消费。
 * */
@Getter
public final class BalanceReservation {

    /** 预留标识 */
    private final BalanceReservationId reservationId;

    /** 所属余额标识 */
    private final BalanceId balanceId;

    /** 预留状态 */
    private BalanceReservationStatus status;

    /** 含币种的正数金额 */
    private final Money amount;

    /** 业务引用类型 */
    private final String referenceType;

    /** 业务引用标识 */
    private final String referenceId;

    /** 创建时间 */
    private final Instant createdAt;

    /** 最后更新时间 */
    private Instant updatedAt;

    @Builder
    private BalanceReservation(BalanceReservationId reservationId, BalanceId balanceId,
                              BalanceReservationStatus status, Money amount,
                              String referenceType, String referenceId,
                              Instant createdAt, Instant updatedAt) {
        if (reservationId == null || balanceId == null || status == null || createdAt == null
                || referenceType == null || referenceType.isBlank()
                || referenceId == null || referenceId.isBlank()
                || updatedAt == null) {
            throw new BalanceDomainException(BalanceErrorCode.INVALID_PARAMS);
        }
        if (amount == null || amount.amount().signum() <= 0) {
            throw new BalanceDomainException(BalanceErrorCode.INVALID_AMOUNT);
        }
        this.reservationId = reservationId;
        this.balanceId = balanceId;
        this.status = status;
        this.amount = amount;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /** 释放生效中的预留。 */
    public void release() {
        requireReservedState();
        status = BalanceReservationStatus.RELEASED;
        updatedAt = Instant.now();
    }

    /** 消费生效中的预留。 */
    public void consume() {
        requireReservedState();
        status = BalanceReservationStatus.CONSUMED;
        updatedAt = Instant.now();
    }

    private void requireReservedState() {
        if (status != BalanceReservationStatus.RESERVED) {
            throw new BalanceDomainException(BalanceErrorCode.INVALID_RESERVATION_STATE);
        }
    }
}
