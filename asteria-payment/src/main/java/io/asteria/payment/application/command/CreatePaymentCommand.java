package io.asteria.payment.application.command;

import io.asteria.common.domain.valueobject.Money;
import io.asteria.payment.domain.enums.PaymentMethod;
import io.asteria.payment.domain.valueobject.PaymentReference;

/**
 * 创建支付命令
 * */
public record CreatePaymentCommand(
        Long merchantId,
        Money amount,
        PaymentMethod paymentMethod,
        PaymentReference reference
) {
}