package io.asteria.balance.domain.valueobject;

import io.asteria.balance.domain.error.BalanceErrorCode;
import io.asteria.balance.domain.exception.BalanceDomainException;

/** 余额领域标识。 */
public record BalanceMovementId(Long value) {
    public BalanceMovementId {
        if (value == null || value <= 0) {
            throw new BalanceDomainException(BalanceErrorCode.INVALID_PARAMS);
        }
    }

    public static BalanceMovementId of(Long value) {
        return new BalanceMovementId(value);
    }
}
