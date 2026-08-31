package io.asteria.settlement.application.command;

import io.asteria.common.domain.valueobject.Money;
import lombok.Builder;

/**
 * 创建结算明细命令。
 */
@Builder
public record CreateSettlementItemCommand(
        Long paymentId,
        String paymentReference,
        Money amount
) {
}