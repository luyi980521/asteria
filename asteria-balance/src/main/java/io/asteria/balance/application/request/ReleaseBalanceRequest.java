package io.asteria.balance.application.request;

import lombok.Builder;

/** Balance HTTP request owned by the receiving module. The amount comes from the existing reservation. */
@Builder
public record ReleaseBalanceRequest(
        Long merchantId,
        String currency,
        String referenceType,
        String referenceId,
        Long eventId
) {
}
