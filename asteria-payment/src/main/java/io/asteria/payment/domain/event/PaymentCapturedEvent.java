package io.asteria.payment.domain.event;

import io.asteria.common.domain.valueobject.Money;
import lombok.Builder;

import java.time.Instant;

/**
 * 支付捕获成功事件。
 */
@Builder
public record PaymentCapturedEvent(
        String eventId,
        Long paymentId,
        Reference paymentReference,
        Money amount,
        Instant capturedAt
) {
    /** Wire representation retaining both parts of the payment's business reference. */
    @Builder
    public record Reference(String referenceType, String referenceId) {
    }
}
