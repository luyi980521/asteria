package io.asteria.ledger.domain.valueobject;

import io.asteria.ledger.domain.error.LedgerErrorCode;
import io.asteria.ledger.domain.exception.LedgerDomainException;

public record AccountId(Long value) {

    public AccountId {
        if (value == null) {
            throw new LedgerDomainException(LedgerErrorCode.NULL_ARGUMENT);
        }
        if (value <= 0) {
            throw new LedgerDomainException(LedgerErrorCode.INVALID_PARAMS);
        }
    }

    public static AccountId of(Long value) {
        return new AccountId(value);
    }
}
