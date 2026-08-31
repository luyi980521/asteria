package io.asteria.settlement.application.command;

import io.asteria.common.domain.valueobject.Money;
import lombok.Builder;

import java.util.List;

/**
 * 创建结算批次命令。
 */
@Builder
public record CreateSettlementBatchCommand(
        String reference,
        Money grossAmount,
        Money feeAmount,
        Money netAmount,
        List<CreateSettlementItemCommand> items
) {
}