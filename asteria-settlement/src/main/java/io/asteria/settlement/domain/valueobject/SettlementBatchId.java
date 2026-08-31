package io.asteria.settlement.domain.valueobject;

import lombok.Builder;

/**
 * 结算批次 ID
 */
@Builder
public record SettlementBatchId(
        Long value
) {
}