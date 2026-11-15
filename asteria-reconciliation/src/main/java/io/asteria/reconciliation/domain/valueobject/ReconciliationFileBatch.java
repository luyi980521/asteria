package io.asteria.reconciliation.domain.valueobject;

import io.asteria.reconciliation.domain.enums.ReconciliationFileBatchStatus;
import lombok.Builder;
import java.time.Instant;
import java.time.LocalDate;

/**
 * 单个文件的持久化元数据，使用 batchId 关联多文件交付，不实现业务状态转换。
 */
@Builder
public record ReconciliationFileBatch(
    /* 应用分配的文件主键 */
    Long fileId,

    /* 所属交付批次标识，可空 */
    Long batchId,

    /* 来源系统 */
    String sourceSystem,

    /* 对象存储相对路径 */
    String objectKey,

    /* 文件记录类型 */
    String recordType,

    /* 文件交付版本 */
    Long fileVersion,

    /* 数据格式版本 */
    Integer schemaVersion,

    /* 来源约定的业务日期，可空 */
    LocalDate businessDate,

    /* 包含的业务窗口起点 */
    Instant windowStart,

    /* 不包含的业务窗口终点 */
    Instant windowEnd,

    /* 采集可见截止时间，可空 */
    Instant cutoffAt,

    /* 文件原始字节的 SHA-256 */
    String sha256,

    /* 文件字节数 */
    Long byteSize,

    /* 预期数据行数，不含表头 */
    Long recordCount,

    /* 实际处理行数，处理前可空 */
    Long actualRecordCount,

    /* 文件批次状态 */
    ReconciliationFileBatchStatus status,

    /* 失败说明，可空 */
    String failureReason,

    /* 结束时间，可空 */
    Instant completedAt,

    /* 创建时间 */
    Instant createdAt,

    /* 最近更新时间 */
    Instant updatedAt) { }