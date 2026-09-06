package io.asteria.settlement.application.port.channel;

import io.asteria.common.domain.valueobject.CurrencyCode;

import io.asteria.common.domain.valueobject.Money;
import lombok.Builder;

import java.util.List;

@Builder
public record SettlementBatchRequest(
        Long settlementBatchId,
        String settlementBatchReference,
        CurrencyCode currency,
        Money grossAmount,
        Money feeAmount,
        Money netAmount,
        List<SettlementItemRequest> items
) {
}