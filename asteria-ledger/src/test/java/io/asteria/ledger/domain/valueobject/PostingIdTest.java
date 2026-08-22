package io.asteria.ledger.domain.valueobject;

import io.asteria.ledger.domain.error.LedgerErrorCode;
import io.asteria.ledger.domain.exception.LedgerDomainException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PostingIdTest {

    @Test
    void ofPreservesLongValue() {
        assertEquals(2001L, PostingId.of(2001L).value());
    }

    @Test
    void nullValueReturnsNullArgument() {
        LedgerDomainException exception = assertThrows(
                LedgerDomainException.class,
                () -> PostingId.of(null));

        assertEquals(LedgerErrorCode.NULL_ARGUMENT, exception.errorCode());
    }

    @Test
    void nonPositiveValueIsRejected() {
        assertThrows(LedgerDomainException.class, () -> PostingId.of(0L));
        assertThrows(LedgerDomainException.class, () -> PostingId.of(-1L));
    }

    @Test
    void sameValuesAreEqual() {
        assertEquals(PostingId.of(2001L), PostingId.of(2001L));
    }

    @Test
    void differentIdTypesAreNotEqual() {
        assertNotEquals(PostingId.of(2001L), LedgerAccountId.of(2001L));
    }
}
