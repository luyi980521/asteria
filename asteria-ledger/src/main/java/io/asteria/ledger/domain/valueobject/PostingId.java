package io.asteria.ledger.domain.valueobject;

import io.asteria.ledger.domain.error.LedgerErrorCode;
import io.asteria.ledger.domain.exception.LedgerDomainException;

import java.util.UUID;

public record PostingId(UUID value) {

    public PostingId {
        if (value == null) {
            throw new LedgerDomainException(LedgerErrorCode.NULL_ARGUMENT);
        }
    }

    public static PostingId generate() {
        return new PostingId(UUID.randomUUID());
    }

    public static PostingId from(UUID value) {
        return new PostingId(value);
    }
}
