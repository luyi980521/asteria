package io.asteria.ledger.domain.valueobject;

import io.asteria.ledger.domain.error.LedgerErrorCode;
import io.asteria.ledger.domain.exception.LedgerDomainException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PostingIdTest {

    @Test
    void generateReturnsNonNullUuid() {
        assertTrue(PostingId.generate().value() != null);
    }

    @Test
    void fromPreservesUuid() {
        UUID uuid = UUID.randomUUID();

        assertEquals(uuid, PostingId.from(uuid).value());
    }

    @Test
    void nullUuidReturnsNullArgument() {
        LedgerDomainException exception = assertThrows(LedgerDomainException.class, () -> PostingId.from(null));

        assertEquals(LedgerErrorCode.NULL_ARGUMENT, exception.errorCode());
    }

    @Test
    void sameUuidIdsAreEqual() {
        UUID uuid = UUID.randomUUID();

        assertEquals(PostingId.from(uuid), PostingId.from(uuid));
    }

    @Test
    void differentIdTypesAreNotEqual() {
        UUID uuid = UUID.randomUUID();

        assertNotEquals(PostingId.from(uuid), LedgerAccountId.from(uuid));
    }
}
