package io.asteria.payment.valueobject;

import org.apache.commons.lang3.StringUtils;

/**
 * 支付业务关联信息
 * */
public record PaymentReference(String referenceType, String referenceId) {

    /**
     * 创建支付业务关联信息
     * */
    public PaymentReference {
        if (StringUtils.isBlank(referenceType)) {
            throw new IllegalArgumentException("Payment reference type must not be blank");
        }

        if (StringUtils.isBlank(referenceId)) {
            throw new IllegalArgumentException("Payment reference id must not be blank");
        }
    }
}