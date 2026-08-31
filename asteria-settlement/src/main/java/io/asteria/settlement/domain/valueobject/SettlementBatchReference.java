package io.asteria.settlement.domain.valueobject;

import lombok.Builder;

/**
 * 结算业务引用
 */
@Builder
public record SettlementBatchReference(
        String value
) {
}