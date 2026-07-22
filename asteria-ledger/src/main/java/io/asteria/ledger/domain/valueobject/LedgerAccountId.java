package io.asteria.ledger.domain.valueobject;

import io.asteria.ledger.domain.error.LedgerErrorCode;
import io.asteria.ledger.domain.exception.LedgerDomainException;

import java.util.UUID;

public record LedgerAccountId(UUID value) {

    public LedgerAccountId {
        if (value == null) {
            throw new LedgerDomainException(LedgerErrorCode.NULL_ARGUMENT);
        }
    }

    public static LedgerAccountId generate() {
        return new LedgerAccountId(UUID.randomUUID());
    }

    public static LedgerAccountId from(UUID value) {
        return new LedgerAccountId(value);
    }
}
