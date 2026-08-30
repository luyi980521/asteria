package io.asteria.payment.domain.event;

import io.asteria.common.domain.valueobject.Money;
import io.asteria.payment.domain.valueobject.PaymentId;
import io.asteria.payment.domain.valueobject.PaymentReference;

import java.time.Instant;

/**
 * 支付捕获成功事件。
 */
public record PaymentCapturedEvent(
        String eventId,
        PaymentId paymentId,
        PaymentReference paymentReference,
        Money amount,
        Instant capturedAt
) {
}