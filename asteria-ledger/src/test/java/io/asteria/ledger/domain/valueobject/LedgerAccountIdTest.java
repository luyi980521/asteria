package io.asteria.ledger.domain.valueobject;

import io.asteria.ledger.domain.error.LedgerErrorCode;
import io.asteria.ledger.domain.exception.LedgerDomainException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LedgerAccountIdTest {

    @Test
    void generateReturnsNonNullUuid() {
        assertTrue(LedgerAccountId.generate().value() != null);
    }

    @Test
    void fromPreservesUuid() {
        UUID uuid = UUID.randomUUID();

        assertEquals(uuid, LedgerAccountId.from(uuid).value());
    }

    @Test
    void nullUuidReturnsNullArgument() {
        LedgerDomainException exception = assertThrows(LedgerDomainException.class, () -> LedgerAccountId.from(null));

        assertEquals(LedgerErrorCode.NULL_ARGUMENT, exception.errorCode());
    }

    @Test
    void sameUuidIdsAreEqual() {
        UUID uuid = UUID.randomUUID();

        assertEquals(LedgerAccountId.from(uuid), LedgerAccountId.from(uuid));
    }

    @Test
    void differentIdTypesAreNotEqual() {
        UUID uuid = UUID.randomUUID();

        assertNotEquals(LedgerAccountId.from(uuid), JournalEntryId.from(uuid));
    }
}
