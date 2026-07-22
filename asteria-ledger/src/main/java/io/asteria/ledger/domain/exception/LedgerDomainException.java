package io.asteria.ledger.domain.exception;

import io.asteria.ledger.domain.error.LedgerErrorCode;

public class LedgerDomainException extends RuntimeException {

    private final LedgerErrorCode errorCode;

    public LedgerDomainException(LedgerErrorCode errorCode) {
        this(requireErrorCode(errorCode), requireErrorCode(errorCode).defaultMessage(), null);
    }

    public LedgerDomainException(LedgerErrorCode errorCode, String detailMessage) {
        this(requireErrorCode(errorCode), detailMessage, null);
    }

    public LedgerDomainException(LedgerErrorCode errorCode, Throwable cause) {
        this(requireErrorCode(errorCode), requireErrorCode(errorCode).defaultMessage(), cause);
    }

    private LedgerDomainException(LedgerErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public LedgerErrorCode errorCode() {
        return errorCode;
    }

    private static LedgerErrorCode requireErrorCode(LedgerErrorCode errorCode) {
        if (errorCode == null) {
            throw new LedgerDomainException(LedgerErrorCode.NULL_ARGUMENT, "errorCode must not be null");
        }
        return errorCode;
    }
}
