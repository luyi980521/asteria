package io.asteria.settlement.application.assembler;

import io.asteria.common.application.port.DistributedIdGenerator;
import io.asteria.settlement.application.command.CreateSettlementBatchCommand;
import io.asteria.settlement.application.command.CreateSettlementItemCommand;
import io.asteria.settlement.application.port.channel.SettlementBatchRequest;
import io.asteria.settlement.application.port.channel.SettlementItemRequest;
import io.asteria.settlement.domain.entity.SettlementBatch;
import io.asteria.settlement.domain.entity.SettlementItem;
import io.asteria.settlement.domain.valueobject.SettlementBatchId;
import io.asteria.settlement.domain.valueobject.SettlementBatchReference;
import io.asteria.settlement.domain.valueobject.SettlementItemId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
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

    public SettlementBatchRequest toSettlementBatchRequest(SettlementBatch settlementBatch) {
        List<SettlementItemRequest> items = new ArrayList<>();
        for (SettlementItem item : settlementBatch.getItems()) {
            SettlementItemRequest newItem = SettlementItemRequest.builder()
                    .paymentId(item.getPaymentId())
                    .paymentReference(item.getPaymentReference())
                    .amount(item.getAmount())
                    .build();
            items.add(newItem);
        }

        return SettlementBatchRequest.builder()
                .settlementBatchId(settlementBatch.getSettlementBatchId().value())
                .currency(settlementBatch.getCurrency())
                .grossAmount(settlementBatch.getGrossAmount())
                .feeAmount(settlementBatch.getFeeAmount())
                .netAmount(settlementBatch.getNetAmount())
                .items(items)
                .build();
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