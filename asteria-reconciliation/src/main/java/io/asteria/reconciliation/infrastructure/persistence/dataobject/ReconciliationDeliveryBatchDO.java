package io.asteria.reconciliation.infrastructure.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.Instant;
import java.time.LocalDate;

/**
 * reconciliation_delivery_batch 数据库行，不承担业务策略。
 */
@Data
@TableName("reconciliation_delivery_batch")
public class ReconciliationDeliveryBatchDO {
    /** 应用分配的交付批次主键 */
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    /** 来源系统 */
    private String sourceSystem;

    /** 独立 Manifest 对象键 */
    private String manifestObjectKey;

    /** 交付版本 */
    private Long deliveryVersion;

    /** 清单格式版本 */
    private Integer schemaVersion;

    /** 来源系统约定的业务日期 */
    private LocalDate businessDate;

    /** 包含的业务窗口起点 */
    private Instant windowStart;

    /** 不包含的业务窗口终点 */
    private Instant windowEnd;

    /** 采集可见截止时间 */
    private Instant cutoffAt;

    /** 全部文件预期记录数之和 */
    private Long expectedRecordCount;

    /** 实际处理记录数，处理前可空 */
    private Long actualRecordCount;

    /** 交付状态，不在模型中实现转换 */
    private String status;

    /** 失败说明，可空 */
    private String failureReason;

    /** 结束时间，可空 */
    private Instant completedAt;

    /** 创建时间 */
    private Instant createdAt;

    /** 最近更新时间 */
    private Instant updatedAt;
}