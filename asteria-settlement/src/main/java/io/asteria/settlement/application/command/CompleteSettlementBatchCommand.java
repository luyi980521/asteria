package io.asteria.settlement.application.command;

import lombok.Builder;

import java.time.Instant;

/**
 * 结算完成命令。
 */
@Builder
public record CompleteSettlementBatchCommand(
        String channelSettlementBatchId,
        Instant settledAt
) {
}