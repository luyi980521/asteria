package io.asteria.payment.valueobject;

/**
 * 支付ID
 * */
public record PaymentId(Long value) {

    /**
     * 创建支付ID
     * */
    public static PaymentId of(Long value) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException("Payment id must be positive");
        }

        return new PaymentId(value);
    }
}