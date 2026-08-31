package io.asteria.settlement.domain.valueobject;

import lombok.Builder;

/**
 * 结算批次明细 ID
 */
@Builder
public record SettlementItemId(
        Long value
) {
}