package io.asteria.reconciliation.infrastructure.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.Instant;
import java.time.LocalDate;

/**
 * reconciliation_file_batch 数据库行，不承担业务策略。
 */
@Data
@TableName("reconciliation_file_batch")
public class ReconciliationFileBatchDO {
    /** 应用分配的文件主键 */
    @TableId(value = "file_id", type = IdType.INPUT)
    private Long fileId;

    /** 对象存储相对路径 */
    private String objectKey;

    /** 来源系统 */
    private String sourceSystem;

    /** 文件记录类型 */
    private String recordType;

    /** 包含的业务窗口起点 */
    private Instant windowStart;

    /** 不包含的业务窗口终点 */
    private Instant windowEnd;

    /** 文件交付版本 */
    private Long fileVersion;

    /** 数据格式版本 */
    private Integer schemaVersion;

    /** 文件原始字节的 SHA-256 */
    private String sha256;

    /** 预期数据行数，不含表头 */
    private Long recordCount;

    /** 文件字节数 */
    private Long byteSize;

    /** 文件批次状态 */
    private String status;

    /** 失败说明，可空 */
    private String failureReason;

    /** 创建时间 */
    private Instant createdAt;

    /** 结束时间，可空 */
    private Instant completedAt;

    /** 所属交付批次标识，历史记录可空 */
    private Long batchId;

    /** 来源约定的业务日期，历史记录可空 */
    private LocalDate businessDate;

    /** 采集可见截止时间，历史记录可空 */
    private Instant cutoffAt;

    /** 实际处理行数，处理前可空 */
    private Long actualRecordCount;

    /** 最近更新时间 */
    private Instant updatedAt;
}