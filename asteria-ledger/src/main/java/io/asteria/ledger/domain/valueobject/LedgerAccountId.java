package io.asteria.ledger.domain.valueobject;

import io.asteria.ledger.domain.error.LedgerErrorCode;
import io.asteria.ledger.domain.exception.LedgerDomainException;

public record LedgerAccountId(Long value) {

    public LedgerAccountId {
        if (value == null) {
            throw new LedgerDomainException(LedgerErrorCode.NULL_ARGUMENT);
        }
        if (value <= 0) {
            throw new LedgerDomainException(LedgerErrorCode.INVALID_PARAMS);
        }
    }

    public static LedgerAccountId of(Long value) {
        return new LedgerAccountId(value);
    }
}
