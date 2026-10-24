package io.asteria.payment.application.request;

import lombok.Builder;

import java.math.BigDecimal;

/** Payment-owned request for the Balance HTTP client. */
@Builder
public record ReserveBalanceRequest(
        Long merchantId,
        String currency,
        BigDecimal amount,
        String referenceType,
        String referenceId,
        Long eventId
) {
}
