package io.asteria.common.domain.exception;

import io.asteria.common.domain.error.CommonErrorCode;

public class CommonDomainException extends RuntimeException {

    private final CommonErrorCode errorCode;

    public CommonDomainException(CommonErrorCode errorCode) {
        this(requireErrorCode(errorCode), requireErrorCode(errorCode).defaultMessage(), null);
    }

    public CommonDomainException(CommonErrorCode errorCode, String detailMessage) {
        this(requireErrorCode(errorCode), detailMessage, null);
    }

    public CommonDomainException(CommonErrorCode errorCode, Throwable cause) {
        this(requireErrorCode(errorCode), requireErrorCode(errorCode).defaultMessage(), cause);
    }

    private CommonDomainException(CommonErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public CommonErrorCode errorCode() {
        return errorCode;
    }

    private static CommonErrorCode requireErrorCode(CommonErrorCode errorCode) {
        if (errorCode == null) {
            throw new CommonDomainException(CommonErrorCode.NULL_ARGUMENT, "errorCode must not be null");
        }
        return errorCode;
    }
}
