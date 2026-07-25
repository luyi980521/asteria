package io.asteria.ledger.domain.valueobject;

import io.asteria.ledger.domain.error.LedgerErrorCode;
import io.asteria.ledger.domain.exception.LedgerDomainException;

public record JournalEntryId(Long value) {

    public JournalEntryId {
        if (value == null) {
            throw new LedgerDomainException(LedgerErrorCode.NULL_ARGUMENT);
        }
        if (value <= 0) {
            throw new LedgerDomainException(LedgerErrorCode.INVALID_PARAMS);
        }
    }

    public static JournalEntryId of(Long value) {
        return new JournalEntryId(value);
    }
}
