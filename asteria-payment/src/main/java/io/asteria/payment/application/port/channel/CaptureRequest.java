package io.asteria.payment.application.port.channel;

import io.asteria.common.domain.valueobject.Money;
import io.asteria.payment.domain.valueobject.PaymentId;
import lombok.Builder;

/**
 * 支付捕获请求
 */
@Builder
public record CaptureRequest(
        PaymentId paymentId,
        Money amount,
        String authorizationTransactionId
) {
}