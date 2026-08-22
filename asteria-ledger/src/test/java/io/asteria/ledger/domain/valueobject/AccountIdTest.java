package io.asteria.ledger.domain.valueobject;

import io.asteria.ledger.domain.error.LedgerErrorCode;
import io.asteria.ledger.domain.exception.LedgerDomainException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AccountIdTest {

    @Test
    void ofPreservesLongValue() {
        assertEquals(3001L, AccountId.of(3001L).value());
    }

    @Test
    void nullValueReturnsNullArgument() {
        LedgerDomainException exception = assertThrows(
                LedgerDomainException.class,
                () -> AccountId.of(null));

        assertEquals(LedgerErrorCode.NULL_ARGUMENT, exception.errorCode());
    }

    @Test
    void nonPositiveValueIsRejected() {
        assertThrows(LedgerDomainException.class, () -> AccountId.of(0L));
        assertThrows(LedgerDomainException.class, () -> AccountId.of(-1L));
    }

    @Test
    void sameValuesAreEqual() {
        assertEquals(AccountId.of(3001L), AccountId.of(3001L));
    }

    @Test
    void differentIdTypesAreNotEqual() {
        assertNotEquals(AccountId.of(3001L), JournalEntryId.of(3001L));
    }
}
