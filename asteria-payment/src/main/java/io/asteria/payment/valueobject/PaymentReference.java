package io.asteria.payment.valueobject;

import io.asteria.payment.domain.error.PaymentErrorCode;
import io.asteria.payment.domain.exception.PaymentDomainException;
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
            throw new PaymentDomainException(PaymentErrorCode.PAYMENT_REFERENCE_TYPE_REQUIRED);
        }

        if (StringUtils.isBlank(referenceId)) {
            throw new PaymentDomainException(PaymentErrorCode.PAYMENT_REFERENCE_ID_REQUIRED);
        }
    }
}
