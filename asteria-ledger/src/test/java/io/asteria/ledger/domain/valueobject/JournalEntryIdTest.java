package io.asteria.ledger.domain.valueobject;

import io.asteria.ledger.domain.error.LedgerErrorCode;
import io.asteria.ledger.domain.exception.LedgerDomainException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JournalEntryIdTest {

    @Test
    void generateReturnsNonNullUuid() {
        assertTrue(JournalEntryId.generate().value() != null);
    }

    @Test
    void fromPreservesUuid() {
        UUID uuid = UUID.randomUUID();

        assertEquals(uuid, JournalEntryId.from(uuid).value());
    }

    @Test
    void nullUuidReturnsNullArgument() {
        LedgerDomainException exception = assertThrows(LedgerDomainException.class, () -> JournalEntryId.from(null));

        assertEquals(LedgerErrorCode.NULL_ARGUMENT, exception.errorCode());
    }

    @Test
    void sameUuidIdsAreEqual() {
        UUID uuid = UUID.randomUUID();

        assertEquals(JournalEntryId.from(uuid), JournalEntryId.from(uuid));
    }

    @Test
    void differentIdTypesAreNotEqual() {
        UUID uuid = UUID.randomUUID();

        assertNotEquals(JournalEntryId.from(uuid), PostingId.from(uuid));
    }
}
