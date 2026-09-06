package io.asteria.settlement.infrastructure.persistence.converter;

import io.asteria.common.domain.valueobject.CurrencyCode;
import io.asteria.common.domain.valueobject.Money;
import io.asteria.settlement.domain.entity.SettlementBatch;
import io.asteria.settlement.domain.entity.SettlementItem;
import io.asteria.settlement.domain.enums.SettlementBatchStatus;
import io.asteria.settlement.domain.valueobject.SettlementBatchId;
import io.asteria.settlement.domain.valueobject.SettlementBatchReference;
import io.asteria.settlement.domain.valueobject.SettlementItemId;
import io.asteria.settlement.infrastructure.persistence.dataobject.SettlementBatchDO;
import io.asteria.settlement.infrastructure.persistence.dataobject.SettlementItemDO;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * Settlement 持久化转换器。
 */
@Component
public class SettlementPersistenceConverter {

    /**
     * 转换为结算批次持久化对象。
     */
    public SettlementBatchDO toBatchDO(SettlementBatch settlementBatch) {
        return SettlementBatchDO.builder()
                .id(settlementBatch.getSettlementBatchId().value())
                .settlementReference(settlementBatch.getReference().value())
                .currency(settlementBatch.getCurrency().value())
                .grossAmount(settlementBatch.getGrossAmount().amount())
                .feeAmount(settlementBatch.getFeeAmount().amount())
                .netAmount(settlementBatch.getNetAmount().amount())
                .status(settlementBatch.getStatus().name())
                .channelSettlementBatchId(settlementBatch.getChannelSettlementBatchId())
                .createdAt(settlementBatch.getCreatedAt())
                .processingAt(settlementBatch.getProcessingAt())
                .acceptedAt(settlementBatch.getAcceptedAt())
                .settledAt(settlementBatch.getSettledAt())
                .failedAt(settlementBatch.getFailedAt())
                .updatedAt(Instant.now())
                .build();
    }

    /**
     * 转换为结算明细持久化对象。
     */
    public SettlementItemDO toItemDO(SettlementBatchId settlementBatchId, SettlementItem item) {
        return SettlementItemDO.builder()
                .id(item.getSettlementItemId().value())
                .settlementBatchId(settlementBatchId.value())
                .paymentId(item.getPaymentId())
                .paymentReference(item.getPaymentReference())
                .amount(item.getAmount().amount())
                .currency(item.getAmount().currency().value())
                .createdAt(item.getCreatedAt())
                .build();
    }

    /**
     * 转换为 Settlement Batch 聚合。
     */
    public SettlementBatch toDomain(SettlementBatchDO batchDO, List<SettlementItemDO> itemDOList) {
        List<SettlementItem> items = itemDOList.stream()
                .map(this::toItemDomain)
                .toList();

        return SettlementBatch.reconstitute(
                SettlementBatchId.builder()
                        .value(batchDO.getId())
                        .build(),
                SettlementBatchReference.builder()
                        .value(batchDO.getSettlementReference())
                        .build(),
                CurrencyCode.of(batchDO.getCurrency()),
                Money.of(batchDO.getGrossAmount(), CurrencyCode.of(batchDO.getCurrency())),
                Money.of(batchDO.getFeeAmount(), CurrencyCode.of(batchDO.getCurrency())),
                Money.of(batchDO.getNetAmount(), CurrencyCode.of(batchDO.getCurrency())),
                items,
                SettlementBatchStatus.valueOf(batchDO.getStatus()),
                batchDO.getChannelSettlementBatchId(),
                batchDO.getCreatedAt(),
                batchDO.getProcessingAt(),
                batchDO.getAcceptedAt(),
                batchDO.getSettledAt(),
                batchDO.getFailedAt()
        );
    }

    /**
     * 转换为 Settlement Item 领域对象。
     */
    private SettlementItem toItemDomain(SettlementItemDO itemDO) {
        return SettlementItem.builder()
                .settlementItemId(
                        SettlementItemId.builder()
                                .value(itemDO.getId())
                                .build()
                )
                .paymentId(itemDO.getPaymentId())
                .paymentReference(itemDO.getPaymentReference())
                .amount(Money.of(itemDO.getAmount(), CurrencyCode.of(itemDO.getCurrency())))
                .createdAt(itemDO.getCreatedAt())
                .build();
    }
}