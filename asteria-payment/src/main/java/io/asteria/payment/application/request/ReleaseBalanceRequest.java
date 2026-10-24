package io.asteria.payment.application.request;

import lombok.Builder;

/** Payment-owned request for the Balance HTTP client. The amount comes from the existing reservation. */
@Builder
public record ReleaseBalanceRequest(
        Long merchantId,
        String currency,
        String referenceType,
        String referenceId,
        Long eventId
) {
}
