package io.asteria.ledger.domain.exception;

import io.asteria.ledger.domain.error.LedgerErrorCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LedgerDomainExceptionTest {

    @Test
    void preservesErrorCode() {
        LedgerDomainException exception = new LedgerDomainException(LedgerErrorCode.INVALID_MONEY_AMOUNT);

        assertEquals(LedgerErrorCode.INVALID_MONEY_AMOUNT, exception.errorCode());
    }

    @Test
    void defaultMessageUsesErrorCodeDefaultMessage() {
        LedgerDomainException exception = new LedgerDomainException(LedgerErrorCode.INVALID_MONEY_AMOUNT);

        assertEquals(LedgerErrorCode.INVALID_MONEY_AMOUNT.defaultMessage(), exception.getMessage());
    }

    @Test
    void detailMessageIsPreserved() {
        LedgerDomainException exception = new LedgerDomainException(LedgerErrorCode.INVALID_MONEY_AMOUNT, "detail");

        assertEquals("detail", exception.getMessage());
    }

    @Test
    void causeIsPreserved() {
        RuntimeException cause = new RuntimeException("cause");
        LedgerDomainException exception = new LedgerDomainException(LedgerErrorCode.INVALID_MONEY_AMOUNT, cause);

        assertEquals(cause, exception.getCause());
    }

    @Test
    void nullErrorCodeIsRejected() {
        LedgerDomainException exception = assertThrows(LedgerDomainException.class, () -> new LedgerDomainException(null));

        assertEquals(LedgerErrorCode.NULL_ARGUMENT, exception.errorCode());
    }
}
