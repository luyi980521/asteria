package io.asteria.balance.domain.valueobject;

import io.asteria.balance.domain.error.BalanceErrorCode;
import io.asteria.balance.domain.exception.BalanceDomainException;

/** 余额账户标识，独立于 LedgerAccountId，当前不建立跨模块映射。 */
public record BalanceAccountId(Long value) {
    public BalanceAccountId {
        if (value == null || value <= 0) {
            throw new BalanceDomainException(BalanceErrorCode.INVALID_PARAMS);
        }
    }

    public static BalanceAccountId of(Long value) {
        return new BalanceAccountId(value);
    }
}
