package io.asteria.ledger.application.message;

import io.asteria.common.domain.valueobject.Money;
import lombok.Builder;

import java.time.Instant;

/**
 * 支付捕获事件 payload
 * */
@Builder
public record PaymentCapturedMessage(
        String eventId,
        Long paymentId,
        String paymentReference,
        Money amount,
        Instant capturedAt
) {
}
