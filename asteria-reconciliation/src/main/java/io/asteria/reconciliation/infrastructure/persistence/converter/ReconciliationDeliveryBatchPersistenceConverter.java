package io.asteria.reconciliation.infrastructure.persistence.converter;

import io.asteria.reconciliation.domain.enums.ReconciliationFileBatchStatus;
import io.asteria.reconciliation.domain.entity.ReconciliationDeliveryBatch;
import io.asteria.reconciliation.infrastructure.persistence.dataobject.ReconciliationDeliveryBatchDO;
import io.asteria.reconciliation.domain.valueobject.ReconciliationDeliveryBatchId;
import org.springframework.stereotype.Component;

/**
 * ReconciliationDeliveryBatch 与数据库行的显式转换，不转换业务状态。
 */
@Component
public class ReconciliationDeliveryBatchPersistenceConverter {
    /**
     * 转换持久化行。
     * @param value 领域元数据
     * @return 数据库行
     */
    public ReconciliationDeliveryBatchDO toDataObject(ReconciliationDeliveryBatch value) {
        var row = new ReconciliationDeliveryBatchDO();
        row.setId(value.getBatchId().value());
        row.setSourceSystem(value.getSourceSystem());
        row.setManifestObjectKey(value.getManifestObjectKey());
        row.setDeliveryVersion(value.getFileVersion());
        row.setSchemaVersion(value.getSchemaVersion());
        row.setBusinessDate(value.getBusinessDate());
        row.setWindowStart(value.getWindowStart());
        row.setWindowEnd(value.getWindowEnd());
        row.setCutoffAt(value.getCutoffAt());
        row.setExpectedRecordCount(value.getExpectedRecordCount());
        row.setActualRecordCount(value.getActualRecordCount());
        row.setStatus(value.getStatus().name());
        row.setFailureReason(value.getFailureReason());
        row.setCompletedAt(value.getCompletedAt());
        row.setCreatedAt(value.getCreatedAt());
        row.setUpdatedAt(value.getUpdatedAt());
        return row;
    }
    /**
     * 恢复领域元数据。
     * @param row 数据库行
     * @return 领域快照
     */
    public ReconciliationDeliveryBatch toDomain(ReconciliationDeliveryBatchDO row) {
        return ReconciliationDeliveryBatch.builder()
                .batchId(ReconciliationDeliveryBatchId.of(row.getId()))
                .sourceSystem(row.getSourceSystem())
                .manifestObjectKey(row.getManifestObjectKey())
                .fileVersion(row.getDeliveryVersion())
                .schemaVersion(row.getSchemaVersion())
                .businessDate(row.getBusinessDate())
                .windowStart(row.getWindowStart())
                .windowEnd(row.getWindowEnd())
                .cutoffAt(row.getCutoffAt())
                .expectedRecordCount(row.getExpectedRecordCount())
                .actualRecordCount(row.getActualRecordCount())
                .status(ReconciliationFileBatchStatus.valueOf(row.getStatus()))
                .failureReason(row.getFailureReason())
                .completedAt(row.getCompletedAt())
                .createdAt(row.getCreatedAt())
                .updatedAt(row.getUpdatedAt())
                .build();
    }
}
