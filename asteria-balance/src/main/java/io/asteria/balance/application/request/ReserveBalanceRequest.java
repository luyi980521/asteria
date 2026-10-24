package io.asteria.balance.application.request;

import lombok.Builder;

import java.math.BigDecimal;

/** Balance HTTP request owned by the receiving module. */
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
