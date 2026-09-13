package io.asteria.balance.domain.valueobject;

import io.asteria.balance.domain.error.BalanceErrorCode;
import io.asteria.balance.domain.exception.BalanceDomainException;

/** 余额领域标识。 */
public record BalanceId(Long value) {
    public BalanceId {
        if (value == null || value <= 0) {
            throw new BalanceDomainException(BalanceErrorCode.INVALID_PARAMS);
        }
    }

    public static BalanceId of(Long value) {
        return new BalanceId(value);
    }
}
