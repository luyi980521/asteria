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
        row.setBatchId(value.batchId());
        row.setSourceSystem(value.sourceSystem());
        row.setObjectKey(value.objectKey());
        row.setRecordType(value.recordType());
        row.setFileVersion(value.fileVersion());
        row.setSchemaVersion(value.schemaVersion());
        row.setBusinessDate(value.businessDate());
        row.setWindowStart(value.windowStart());
        row.setWindowEnd(value.windowEnd());
        row.setCutoffAt(value.cutoffAt());
        row.setSha256(value.sha256());
        row.setByteSize(value.byteSize());
        row.setRecordCount(value.recordCount());
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
    public ReconciliationFileBatch toDomain(ReconciliationFileBatchDO row) {
        return ReconciliationFileBatch.builder()
                .fileId(row.getFileId())
                .batchId(row.getBatchId())
                .sourceSystem(row.getSourceSystem())
                .objectKey(row.getObjectKey())
                .recordType(row.getRecordType())
                .fileVersion(row.getFileVersion())
                .schemaVersion(row.getSchemaVersion())
                .businessDate(row.getBusinessDate())
                .windowStart(row.getWindowStart())
                .windowEnd(row.getWindowEnd())
                .cutoffAt(row.getCutoffAt())
                .sha256(row.getSha256())
                .byteSize(row.getByteSize())
                .recordCount(row.getRecordCount())
                .actualRecordCount(row.getActualRecordCount())
                .status(ReconciliationFileBatchStatus.valueOf(row.getStatus()))
                .failureReason(row.getFailureReason())
                .completedAt(row.getCompletedAt())
                .createdAt(row.getCreatedAt())
                .updatedAt(row.getUpdatedAt())
                .build();
    }
}