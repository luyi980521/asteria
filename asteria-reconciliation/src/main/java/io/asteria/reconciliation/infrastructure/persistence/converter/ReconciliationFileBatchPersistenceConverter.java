package io.asteria.reconciliation.infrastructure.persistence.converter;

import io.asteria.reconciliation.domain.enums.ReconciliationFileBatchStatus;
import io.asteria.reconciliation.domain.entity.ReconciliationFileBatch;
import io.asteria.reconciliation.infrastructure.persistence.dataobject.ReconciliationFileBatchDO;
import io.asteria.reconciliation.domain.valueobject.ReconciliationDeliveryBatchId;
import io.asteria.reconciliation.domain.valueobject.ReconciliationFileBatchId;
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
        row.setId(value.getFileId().value());
        row.setBatchId(value.getBatchId() == null ? null : value.getBatchId().value());
        row.setSourceSystem(value.getSourceSystem());
        row.setObjectKey(value.getObjectKey());
        row.setRecordType(value.getRecordType());
        row.setFileVersion(value.getFileVersion());
        row.setSchemaVersion(value.getSchemaVersion());
        row.setBusinessDate(value.getBusinessDate());
        row.setWindowStart(value.getWindowStart());
        row.setWindowEnd(value.getWindowEnd());
        row.setCutoffAt(value.getCutoffAt());
        row.setSha256(value.getSha256());
        row.setByteSize(value.getByteSize());
        row.setRecordCount(value.getRecordCount());
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
    public ReconciliationFileBatch toDomain(ReconciliationFileBatchDO row) {
        return ReconciliationFileBatch.builder()
                .fileId(ReconciliationFileBatchId.of(row.getId()))
                .batchId(row.getBatchId() == null ? null : ReconciliationDeliveryBatchId.of(row.getBatchId()))
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
