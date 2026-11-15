package io.asteria.reconciliation.domain.valueobject;

import io.asteria.reconciliation.domain.enums.ReconciliationFileBatchStatus;
import lombok.Builder;
import java.time.Instant;
import java.time.LocalDate;

/**
 * 多文件交付批次头；独立文件描述存于 reconciliation_file_batch，不执行完整性或业务判断。
 */
@Builder
public record ReconciliationDeliveryBatch(
    /* 应用分配的交付批次主键 */
    Long batchId,

    /* 来源系统 */
    String sourceSystem,

    /* 独立 Manifest 对象键 */
    String manifestObjectKey,

    /* 交付版本 */
    Long fileVersion,

    /* 清单格式版本 */
    Integer schemaVersion,

    /* 来源系统约定的业务日期 */
    LocalDate businessDate,

    /* 包含的业务窗口起点 */
    Instant windowStart,

    /* 不包含的业务窗口终点 */
    Instant windowEnd,

    /* 采集可见截止时间 */
    Instant cutoffAt,

    /* 全部文件预期记录数之和 */
    Long expectedRecordCount,

    /* 实际处理记录数，处理前可空 */
    Long actualRecordCount,

    /* 交付状态，不在模型中实现转换 */
    ReconciliationFileBatchStatus status,

    /* 失败说明，可空 */
    String failureReason,

    /* 结束时间，可空 */
    Instant completedAt,

    /* 创建时间 */
    Instant createdAt,

    /* 最近更新时间 */
    Instant updatedAt) { }