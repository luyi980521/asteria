package io.asteria.settlement.application.port.channel;

import lombok.Builder;

import java.time.Instant;

@Builder
public record SettlementBatchResult(
        boolean accepted,
        String channelSettlementBatchId,
        Instant acceptedAt,
        String failureCode,
        String failureMessage
) {
}