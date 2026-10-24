package io.asteria.reconciliation.infrastructure.persistence.converter;

import io.asteria.common.domain.valueobject.CurrencyCode;
import io.asteria.common.domain.valueobject.Money;
import io.asteria.reconciliation.domain.entity.ReconciliationSourceRecord;
import io.asteria.reconciliation.domain.enums.ReconciliationSourceType;
import io.asteria.reconciliation.domain.valueobject.ReconciliationSourceRecordId;
import io.asteria.reconciliation.infrastructure.persistence.dataobject.ReconciliationSourceRecordDO;
import org.springframework.stereotype.Component;

/**
 * ReconciliationSourceRecord 与数据库行的显式转换；保留 String/TEXT JSON，不重复序列化。
 */
@Component
public class ReconciliationSourceRecordPersistenceConverter {
    /**
     * 将类型化 ID、枚举及 Money 拆分为现有数据库列类型。
     */
    public ReconciliationSourceRecordDO toDataObject(ReconciliationSourceRecord domain) {
        return ReconciliationSourceRecordDO.builder()
                .id(domain.getRecordId().value())
                .sourceType(domain.getSourceType().name())
                .sourceRecordId(domain.getSourceRecordId())
                .sourceVersion(domain.getSourceVersion())
                .businessKey(domain.getBusinessKey())
                .amount(domain.getAmount() == null ? null : domain.getAmount().amount())
                .currency(domain.getAmount() == null ? null : domain.getAmount().currency().value())
                .sourceStatus(domain.getSourceStatus())
                .postingStatus(domain.getPostingStatus())
                .occurredAt(domain.getOccurredAt())
                .payload(domain.getPayload())
                .createdAt(domain.getCreatedAt())
                .build();
    }

    /**
     * 从持久化数据恢复，重新执行 Builder 的基本领域约束。
     */
    public ReconciliationSourceRecord toDomain(ReconciliationSourceRecordDO dataObject) {
        return ReconciliationSourceRecord.builder()
                .recordId(ReconciliationSourceRecordId.of(dataObject.getId()))
                .sourceType(ReconciliationSourceType.valueOf(dataObject.getSourceType()))
                .sourceRecordId(dataObject.getSourceRecordId())
                .sourceVersion(dataObject.getSourceVersion())
                .businessKey(dataObject.getBusinessKey())
                .amount(dataObject.getAmount() == null ? null : Money.ofNonNegative(dataObject.getAmount(), CurrencyCode.of(dataObject.getCurrency())))
                .sourceStatus(dataObject.getSourceStatus())
                .postingStatus(dataObject.getPostingStatus())
                .occurredAt(dataObject.getOccurredAt())
                .payload(dataObject.getPayload())
                .createdAt(dataObject.getCreatedAt())
                .build();
    }
}
