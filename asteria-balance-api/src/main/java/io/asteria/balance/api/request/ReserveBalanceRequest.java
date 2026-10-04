package io.asteria.balance.api.request;

import lombok.Builder;

import java.math.BigDecimal;

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
