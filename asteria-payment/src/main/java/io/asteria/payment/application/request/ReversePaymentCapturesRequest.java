package io.asteria.payment.application.request;

import lombok.Builder;

import java.util.List;

@Builder
public record ReversePaymentCapturesRequest(
        List<String> eventIds,
        String reason
) {
}
