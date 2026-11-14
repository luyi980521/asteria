package io.asteria.reconciliation.infrastructure.persistence.converter;

import io.asteria.reconciliation.domain.enums.ReconciliationFileBatchStatus;
import io.asteria.reconciliation.domain.valueobject.ReconciliationFileBatch;
import io.asteria.reconciliation.infrastructure.persistence.dataobject.ReconciliationFileBatchDO;
import org.springframework.stereotype.Component;

/**
 * ReconciliationFileBatch 与数据库行的显式转换，不转换业务状态。
 */
@Component
public class ReconciliationFileBatchPersistenceConverter {
    /**
     * 转换持久化行。
     * @param value 领域元数据
     * @return 数据库行
     */
    public ReconciliationFileBatchDO toDataObject(ReconciliationFileBatch value) {
        var row = new ReconciliationFileBatchDO();
        row.setFileId(value.fileId());
        row.setObjectKey(value.objectKey());
        row.setSourceSystem(value.sourceSystem());
        row.setRecordType(value.recordType());
        row.setWindowStart(value.windowStart());
        row.setWindowEnd(value.windowEnd());
        row.setFileVersion(value.fileVersion());
        row.setSchemaVersion(value.schemaVersion());
        row.setSha256(value.sha256());
        row.setRecordCount(value.recordCount());
        row.setByteSize(value.byteSize());
        row.setStatus(value.status().name());
        row.setFailureReason(value.failureReason());
        row.setCreatedAt(value.createdAt());
        row.setCompletedAt(value.completedAt());
        row.setBatchId(value.batchId());
        row.setBusinessDate(value.businessDate());
        row.setCutoffAt(value.cutoffAt());
        row.setActualRecordCount(value.actualRecordCount());
        row.setUpdatedAt(value.updatedAt());
        return row;
    }
    /**
     * 恢复领域元数据。
     * @param row 数据库行
     * @return 领域快照
     */
    public ReconciliationFileBatch toDomain(ReconciliationFileBatchDO row) {
        return new ReconciliationFileBatch(
                row.getFileId(),
                row.getObjectKey(),
                row.getSourceSystem(),
                row.getRecordType(),
                row.getWindowStart(),
                row.getWindowEnd(),
                row.getFileVersion(),
                row.getSchemaVersion(),
                row.getSha256(),
                row.getRecordCount(),
                row.getByteSize(),
                ReconciliationFileBatchStatus.valueOf(row.getStatus()),
                row.getFailureReason(),
                row.getCreatedAt(),
                row.getCompletedAt(),
                row.getBatchId(),
                row.getBusinessDate(),
                row.getCutoffAt(),
                row.getActualRecordCount(),
                row.getUpdatedAt());
    }
}