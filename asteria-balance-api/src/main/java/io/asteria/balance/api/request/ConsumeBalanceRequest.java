package io.asteria.balance.api.request;

import lombok.Builder;

/** The amount comes from the existing reservation. */
@Builder
public record ConsumeBalanceRequest(
        Long merchantId,
        String currency,
        String referenceType,
        String referenceId,
        Long eventId
) {
}
