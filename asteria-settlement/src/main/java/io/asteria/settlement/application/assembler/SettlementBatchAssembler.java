package io.asteria.settlement.application.assembler;

import io.asteria.common.application.port.DistributedIdGenerator;
import io.asteria.settlement.application.command.CreateSettlementBatchCommand;
import io.asteria.settlement.application.command.CreateSettlementItemCommand;
import io.asteria.settlement.domain.entity.SettlementBatch;
import io.asteria.settlement.domain.entity.SettlementItem;
import io.asteria.settlement.domain.valueobject.SettlementBatchId;
import io.asteria.settlement.domain.valueobject.SettlementBatchReference;
import io.asteria.settlement.domain.valueobject.SettlementItemId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * Settlement Batch 组装器。
 */
@Component
@RequiredArgsConstructor
public class SettlementBatchAssembler {

    private final DistributedIdGenerator distributedIdGenerator;

    /**
     * 组装结算批次。
     */
    public SettlementBatch toSettlementBatch(CreateSettlementBatchCommand command) {
        SettlementBatchReference reference = SettlementBatchReference.builder()
                .value(command.reference())
                .build();

        List<SettlementItem> items = command.items().stream()
                .map(this::toSettlementItem)
                .toList();

        return SettlementBatch.create(
                SettlementBatchId.builder()
                        .value(distributedIdGenerator.nextId())
                        .build(),
                reference,
                command.grossAmount(),
                command.feeAmount(),
                command.netAmount(),
                items,
                Instant.now()
        );
    }

    /**
     * 组装结算明细。
     */
    private SettlementItem toSettlementItem(CreateSettlementItemCommand command) {
        return SettlementItem.builder()
                .settlementItemId(
                        SettlementItemId.builder()
                                .value(distributedIdGenerator.nextId())
                                .build()
                )
                .paymentId(command.paymentId())
                .paymentReference(command.paymentReference())
                .amount(command.amount())
                .build();
    }
}