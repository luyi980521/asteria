package io.asteria.ledger.domain.valueobject;

import io.asteria.ledger.domain.error.LedgerErrorCode;
import io.asteria.ledger.domain.exception.LedgerDomainException;

import java.util.UUID;

public record JournalEntryId(UUID value) {

    public JournalEntryId {
        if (value == null) {
            throw new LedgerDomainException(LedgerErrorCode.NULL_ARGUMENT);
        }
    }

    public static JournalEntryId generate() {
        return new JournalEntryId(UUID.randomUUID());
    }

    public static JournalEntryId from(UUID value) {
        return new JournalEntryId(value);
    }
}
