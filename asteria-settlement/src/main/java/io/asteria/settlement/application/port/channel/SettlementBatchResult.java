package io.asteria.settlement.application.port.channel;

import io.asteria.settlement.domain.enums.SettlementBatchSubmitStatus;
import lombok.Builder;

import java.time.Instant;

@Builder
public record SettlementBatchResult(
        SettlementBatchSubmitStatus status,
        String channelSettlementBatchId,
        Instant acceptedAt,
        String failureCode,
        String failureMessage
) {
}