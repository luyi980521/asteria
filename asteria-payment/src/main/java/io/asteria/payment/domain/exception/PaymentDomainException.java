package io.asteria.payment.domain.exception;

import io.asteria.payment.domain.error.PaymentErrorCode;

public class PaymentDomainException extends RuntimeException {

    private final PaymentErrorCode errorCode;

    public PaymentDomainException(PaymentErrorCode errorCode) {
        this(requireErrorCode(errorCode), requireErrorCode(errorCode).defaultMessage(), null);
    }

    public PaymentDomainException(PaymentErrorCode errorCode, String detailMessage) {
        this(requireErrorCode(errorCode), detailMessage, null);
    }

    public PaymentDomainException(PaymentErrorCode errorCode, Throwable cause) {
        this(requireErrorCode(errorCode), requireErrorCode(errorCode).defaultMessage(), cause);
    }

    private PaymentDomainException(PaymentErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public PaymentErrorCode errorCode() {
        return errorCode;
    }

    private static PaymentErrorCode requireErrorCode(PaymentErrorCode errorCode) {
        if (errorCode == null) {
            throw new PaymentDomainException(PaymentErrorCode.PAYMENT_ERROR_CODE_REQUIRED,
                    "errorCode must not be null");
        }
        return errorCode;
    }
}
