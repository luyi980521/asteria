package io.asteria.reconciliation.domain.entity;

import io.asteria.reconciliation.domain.enums.ReconciliationFileBatchStatus;
import io.asteria.reconciliation.domain.valueobject.ReconciliationDeliveryBatchId;
import io.asteria.reconciliation.domain.valueobject.ReconciliationFileBatchId;
import lombok.Builder;
import lombok.Getter;
import java.time.Instant;
import java.time.LocalDate;

/**
 * 单个文件的持久化元数据，使用 batchId 关联多文件交付，不实现业务状态转换。
 */
@Getter
public final class ReconciliationFileBatch {

    /** 应用分配的文件主键 */
    private final ReconciliationFileBatchId fileId;

    /** 所属交付批次标识，可空 */
    private final ReconciliationDeliveryBatchId batchId;

    /** 来源系统 */
    private final String sourceSystem;

    /** 对象存储相对路径 */
    private final String objectKey;

    /** 文件记录类型 */
    private final String recordType;

    /** 文件交付版本 */
    private final Long fileVersion;

    /** 数据格式版本 */
    private final Integer schemaVersion;

    /** 来源约定的业务日期，可空 */
    private final LocalDate businessDate;

    /** 包含的业务窗口起点 */
    private final Instant windowStart;

    /** 不包含的业务窗口终点 */
    private final Instant windowEnd;

    /** 采集可见截止时间，可空 */
    private final Instant cutoffAt;

    /** 文件原始字节的 SHA-256 */
    private final String sha256;

    /** 文件字节数 */
    private final Long byteSize;

    /** 预期数据行数，不含表头 */
    private final Long recordCount;

    /** 实际处理行数，处理前可空 */
    private final Long actualRecordCount;

    /** 文件批次状态 */
    private final ReconciliationFileBatchStatus status;

    /** 失败说明，可空 */
    private final String failureReason;

    /** 结束时间，可空 */
    private final Instant completedAt;

    /** 创建时间 */
    private final Instant createdAt;

    /** 最近更新时间 */
    private final Instant updatedAt;

    /**
     * 构造或恢复批次元数据
     */
    @Builder
    public ReconciliationFileBatch(
            ReconciliationFileBatchId fileId,
            ReconciliationDeliveryBatchId batchId,
            String sourceSystem,
            String objectKey,
            String recordType,
            Long fileVersion,
            Integer schemaVersion,
            LocalDate businessDate,
            Instant windowStart,
            Instant windowEnd,
            Instant cutoffAt,
            String sha256,
            Long byteSize,
            Long recordCount,
            Long actualRecordCount,
            ReconciliationFileBatchStatus status,
            String failureReason,
            Instant completedAt,
            Instant createdAt,
            Instant updatedAt) {
        this.fileId = fileId;
        ReconciliationValidation.require(fileId != null);
        this.batchId = batchId;
        this.sourceSystem = sourceSystem;
        this.objectKey = objectKey;
        this.recordType = recordType;
        this.fileVersion = fileVersion;
        this.schemaVersion = schemaVersion;
        this.businessDate = businessDate;
        this.windowStart = windowStart;
        this.windowEnd = windowEnd;
        this.cutoffAt = cutoffAt;
        this.sha256 = sha256;
        this.byteSize = byteSize;
        this.recordCount = recordCount;
        this.actualRecordCount = actualRecordCount;
        this.status = status;
        this.failureReason = failureReason;
        this.completedAt = completedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
