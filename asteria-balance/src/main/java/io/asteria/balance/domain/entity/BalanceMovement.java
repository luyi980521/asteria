package io.asteria.balance.domain.entity;

import io.asteria.balance.domain.enums.BalanceMovementType;
import io.asteria.balance.domain.error.BalanceErrorCode;
import io.asteria.balance.domain.exception.BalanceDomainException;
import io.asteria.balance.domain.valueobject.BalanceId;
import io.asteria.balance.domain.valueobject.BalanceMovementId;
import io.asteria.common.domain.valueobject.Money;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

/**
 * 余额变化审计快照；当前不自动生成流水或执行幂等处理。
 * */
@Getter
public final class BalanceMovement {

    /** 流水标识 */
    private final BalanceMovementId movementId;

    /** 所属余额标识 */
    private final BalanceId balanceId;

    /** 变化类型 */
    private final BalanceMovementType movementType;

    /** 含币种的正数金额 */
    private final Money amount;

    /** 业务引用类型 */
    private final String referenceType;

    /** 业务引用标识 */
    private final String referenceId;

    /** 幂等事件引用，同一余额及变化类型内唯一 */
    private final String eventId;

    /** 创建时间 */
    private final Instant createdAt;

    @Builder
    private BalanceMovement(BalanceMovementId movementId, BalanceId balanceId,
                              BalanceMovementType movementType, Money amount,
                              String referenceType, String referenceId,
                              String eventId, Instant createdAt) {
        if (movementId == null || balanceId == null || movementType == null || createdAt == null
                || referenceType == null || referenceType.isBlank()
                || referenceId == null || referenceId.isBlank()
                || eventId == null || eventId.isBlank()) {
            throw new BalanceDomainException(BalanceErrorCode.INVALID_PARAMS);
        }
        if (amount == null || amount.amount().signum() <= 0) {
            throw new BalanceDomainException(BalanceErrorCode.INVALID_AMOUNT);
        }
        this.movementId = movementId;
        this.balanceId = balanceId;
        this.movementType = movementType;
        this.amount = amount;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
        this.eventId = eventId;
        this.createdAt = createdAt;
    }

    /**
     * 创建可用余额入账流水
     * */
    public static BalanceMovement create(BalanceMovementId balanceMovementId, BalanceId balanceId,
                                         BalanceMovementType movementType, Money amount, String referenceType,
                                         String referenceId, String eventId) {
        return BalanceMovement.builder()
                .movementId(balanceMovementId)
                .balanceId(balanceId)
                .movementType(movementType)
                .amount(amount)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .eventId(eventId)
                .createdAt(Instant.now())
                .build();
    }
}
