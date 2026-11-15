package io.asteria.reconciliation.infrastructure.persistence.converter;

import io.asteria.reconciliation.domain.enums.ReconciliationFileBatchStatus;
import io.asteria.reconciliation.domain.valueobject.ReconciliationDeliveryBatch;
import io.asteria.reconciliation.infrastructure.persistence.dataobject.ReconciliationDeliveryBatchDO;
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
        row.setId(value.batchId());
        row.setSourceSystem(value.sourceSystem());
        row.setManifestObjectKey(value.manifestObjectKey());
        row.setDeliveryVersion(value.fileVersion());
        row.setSchemaVersion(value.schemaVersion());
        row.setBusinessDate(value.businessDate());
        row.setWindowStart(value.windowStart());
        row.setWindowEnd(value.windowEnd());
        row.setCutoffAt(value.cutoffAt());
        row.setExpectedRecordCount(value.expectedRecordCount());
        row.setActualRecordCount(value.actualRecordCount());
        row.setStatus(value.status().name());
        row.setFailureReason(value.failureReason());
        row.setCompletedAt(value.completedAt());
        row.setCreatedAt(value.createdAt());
        row.setUpdatedAt(value.updatedAt());
        return row;
    }
    /**
     * 恢复领域元数据。
     * @param row 数据库行
     * @return 领域快照
     */
    public ReconciliationDeliveryBatch toDomain(ReconciliationDeliveryBatchDO row) {
        return ReconciliationDeliveryBatch.builder()
                .batchId(row.getId())
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