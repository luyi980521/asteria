package io.asteria.settlement.application.port.channel;

import io.asteria.common.domain.valueobject.Money;
import lombok.Builder;

@Builder
public record SettlementItemRequest(
        Long paymentId,
        String paymentReference,
        Money amount
) {
}