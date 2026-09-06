package io.asteria.currency.domain.exception;

import io.asteria.currency.domain.error.CurrencyErrorCode;

public class CurrencyDomainException extends RuntimeException {

    private final CurrencyErrorCode errorCode;

    public CurrencyDomainException(CurrencyErrorCode errorCode) {
        this(requireErrorCode(errorCode), requireErrorCode(errorCode).defaultMessage(), null);
    }

    public CurrencyDomainException(CurrencyErrorCode errorCode, String detailMessage) {
        this(requireErrorCode(errorCode), detailMessage, null);
    }

    public CurrencyDomainException(CurrencyErrorCode errorCode, Throwable cause) {
        this(requireErrorCode(errorCode), requireErrorCode(errorCode).defaultMessage(), cause);
    }

    private CurrencyDomainException(CurrencyErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public CurrencyErrorCode errorCode() {
        return errorCode;
    }

    private static CurrencyErrorCode requireErrorCode(CurrencyErrorCode errorCode) {
        if (errorCode == null) {
            throw new CurrencyDomainException(CurrencyErrorCode.NULL_ARGUMENT, "errorCode must not be null");
        }
        return errorCode;
    }
}
