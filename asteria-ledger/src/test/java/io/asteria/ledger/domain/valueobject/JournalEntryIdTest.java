package io.asteria.ledger.domain.valueobject;

import io.asteria.ledger.domain.error.LedgerErrorCode;
import io.asteria.ledger.domain.exception.LedgerDomainException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JournalEntryIdTest {

    @Test
    void ofPreservesLongValue() {
        assertEquals(1001L, JournalEntryId.of(1001L).value());
    }

    @Test
    void nullValueReturnsNullArgument() {
        LedgerDomainException exception = assertThrows(
                LedgerDomainException.class,
                () -> JournalEntryId.of(null));

        assertEquals(LedgerErrorCode.NULL_ARGUMENT, exception.errorCode());
    }

    @Test
    void nonPositiveValueIsRejected() {
        assertThrows(LedgerDomainException.class, () -> JournalEntryId.of(0L));
        assertThrows(LedgerDomainException.class, () -> JournalEntryId.of(-1L));
    }

    @Test
    void sameValuesAreEqual() {
        assertEquals(JournalEntryId.of(1001L), JournalEntryId.of(1001L));
    }

    @Test
    void differentIdTypesAreNotEqual() {
        assertNotEquals(JournalEntryId.of(1001L), PostingId.of(1001L));
    }
}
