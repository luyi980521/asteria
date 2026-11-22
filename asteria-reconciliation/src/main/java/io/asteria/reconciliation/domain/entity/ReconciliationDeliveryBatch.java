package io.asteria.reconciliation.domain.entity;

import io.asteria.common.domain.exception.BusinessException;
import io.asteria.reconciliation.domain.enums.ReconciliationFileBatchStatus;
import io.asteria.reconciliation.domain.error.ReconciliationErrorCode;
import io.asteria.reconciliation.domain.valueobject.ReconciliationDeliveryBatchId;
import lombok.Builder;
import lombok.Getter;
import java.time.Instant;
import java.time.LocalDate;

/**
 * 多文件交付批次头；独立文件描述存于 reconciliation_file_batch，不执行完整性或业务判断。
 */
@Getter
public final class ReconciliationDeliveryBatch {

    /** 应用分配的交付批次主键 */
    private final ReconciliationDeliveryBatchId batchId;

    /** 来源系统 */
    private final String sourceSystem;

    /** 独立 Manifest 对象键 */
    private final String manifestObjectKey;

    /** 交付版本 */
    private final Long fileVersion;

    /** 清单格式版本 */
    private final Integer schemaVersion;

    /** 来源系统约定的业务日期 */
    private final LocalDate businessDate;

    /** 包含的业务窗口起点 */
    private final Instant windowStart;

    /** 不包含的业务窗口终点 */
    private final Instant windowEnd;

    /** 采集可见截止时间 */
    private final Instant cutoffAt;

    /** 全部文件预期记录数之和 */
    private final Long expectedRecordCount;

    /** 实际处理记录数，处理前可空 */
    private final Long actualRecordCount;

    /** 交付状态，不在模型中实现转换 */
    private ReconciliationFileBatchStatus status;

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
    public ReconciliationDeliveryBatch(
            ReconciliationDeliveryBatchId batchId,
            String sourceSystem,
            String manifestObjectKey,
            Long fileVersion,
            Integer schemaVersion,
            LocalDate businessDate,
            Instant windowStart,
            Instant windowEnd,
            Instant cutoffAt,
            Long expectedRecordCount,
            Long actualRecordCount,
            ReconciliationFileBatchStatus status,
            String failureReason,
            Instant completedAt,
            Instant createdAt,
            Instant updatedAt) {
        ReconciliationValidation.require(batchId != null);
        this.batchId = batchId;
        this.sourceSystem = sourceSystem;
        this.manifestObjectKey = manifestObjectKey;
        this.fileVersion = fileVersion;
        this.schemaVersion = schemaVersion;
        this.businessDate = businessDate;
        this.windowStart = windowStart;
        this.windowEnd = windowEnd;
        this.cutoffAt = cutoffAt;
        this.expectedRecordCount = expectedRecordCount;
        this.actualRecordCount = actualRecordCount;
        this.status = status;
        this.failureReason = failureReason;
        this.completedAt = completedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public void startValidate() {
        if (status != ReconciliationFileBatchStatus.DELIVERED) {
            throw new BusinessException(ReconciliationErrorCode.INVALID_STATE);
        }
        status = ReconciliationFileBatchStatus.VALIDATING;
    }

    public void validated() {
        if (status != ReconciliationFileBatchStatus.VALIDATING) {
            throw new BusinessException(ReconciliationErrorCode.INVALID_STATE);
        }
        status = ReconciliationFileBatchStatus.VALIDATED;
    }
}
