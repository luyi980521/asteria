package io.asteria.balance.domain.entity;

import io.asteria.balance.domain.enums.BalanceAccountOwnerType;
import io.asteria.balance.domain.enums.BalanceAccountStatus;
import io.asteria.balance.domain.error.BalanceErrorCode;
import io.asteria.balance.domain.exception.BalanceDomainException;
import io.asteria.balance.domain.valueobject.BalanceAccountId;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

/** 余额账户，按所属主体唯一，具体币种及金额由 Balance 维护。 */
@Getter
public final class BalanceAccount {
    private final BalanceAccountId balanceAccountId;
    private final BalanceAccountOwnerType ownerType;
    private final Long ownerId;
    private final BalanceAccountStatus status;
    private final Instant createdAt;
    private final Instant updatedAt;

    @Builder
    private BalanceAccount(BalanceAccountId balanceAccountId, BalanceAccountOwnerType ownerType, Long ownerId,
                           BalanceAccountStatus status, Instant createdAt, Instant updatedAt) {
        if (balanceAccountId == null || ownerType == null || ownerId == null || ownerId <= 0
                || status == null || createdAt == null || updatedAt == null) {
            throw new BalanceDomainException(BalanceErrorCode.INVALID_PARAMS);
        }
        this.balanceAccountId = balanceAccountId;
        this.ownerType = ownerType;
        this.ownerId = ownerId;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /** 创建启用状态的余额账户。 */
    public static BalanceAccount create(BalanceAccountId balanceAccountId, BalanceAccountOwnerType ownerType,
                                        Long ownerId) {
        Instant now = Instant.now();
        return BalanceAccount.builder()
                .balanceAccountId(balanceAccountId)
                .ownerType(ownerType)
                .ownerId(ownerId)
                .status(BalanceAccountStatus.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }
}
