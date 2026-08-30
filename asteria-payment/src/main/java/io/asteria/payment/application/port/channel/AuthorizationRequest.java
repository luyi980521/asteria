package io.asteria.payment.application.port.channel;

import io.asteria.common.domain.valueobject.Money;
import io.asteria.payment.domain.enums.PaymentMethod;
import io.asteria.payment.domain.valueobject.PaymentId;
import lombok.Builder;

/**
 * 支付授权请求
 * */
@Builder
public record AuthorizationRequest(
        PaymentId paymentId,
        Money amount,
        PaymentMethod paymentMethod
) {
}