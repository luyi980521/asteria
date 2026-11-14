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
        row.setBatchId(value.batchId());
        row.setManifestObjectKey(value.manifestObjectKey());
        row.setSourceSystem(value.sourceSystem());
        row.setBusinessDate(value.businessDate());
        row.setWindowStart(value.windowStart());
        row.setWindowEnd(value.windowEnd());
        row.setCutoffAt(value.cutoffAt());
        row.setFileVersion(value.fileVersion());
        row.setSchemaVersion(value.schemaVersion());
        row.setExpectedRecordCount(value.expectedRecordCount());
        row.setActualRecordCount(value.actualRecordCount());
        row.setStatus(value.status().name());
        row.setFailureReason(value.failureReason());
        row.setCreatedAt(value.createdAt());
        row.setUpdatedAt(value.updatedAt());
        row.setCompletedAt(value.completedAt());
        return row;
    }
    /**
     * 恢复领域元数据。
     * @param row 数据库行
     * @return 领域快照
     */
    public ReconciliationDeliveryBatch toDomain(ReconciliationDeliveryBatchDO row) {
        return new ReconciliationDeliveryBatch(
                row.getBatchId(),
                row.getManifestObjectKey(),
                row.getSourceSystem(),
                row.getBusinessDate(),
                row.getWindowStart(),
                row.getWindowEnd(),
                row.getCutoffAt(),
                row.getFileVersion(),
                row.getSchemaVersion(),
                row.getExpectedRecordCount(),
                row.getActualRecordCount(),
                ReconciliationFileBatchStatus.valueOf(row.getStatus()),
                row.getFailureReason(),
                row.getCreatedAt(),
                row.getUpdatedAt(),
                row.getCompletedAt());
    }
}