package io.asteria.payment.valueobject;

import io.asteria.payment.domain.error.PaymentErrorCode;
import io.asteria.payment.domain.exception.PaymentDomainException;

/**
 * 支付ID
 * */
public record PaymentId(Long value) {

    /**
     * 创建支付ID
     * */
    public static PaymentId of(Long value) {
        if (value == null || value <= 0) {
            throw new PaymentDomainException(PaymentErrorCode.PAYMENT_ID_MUST_BE_POSITIVE);
        }

        return new PaymentId(value);
    }
}
