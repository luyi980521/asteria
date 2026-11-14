package io.asteria.reconciliation.application.model;

import lombok.Builder;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * 结算完成事件文件快照，保留总额、费用及净额各自的金额和币种，不计算相互关系。
 */
@Builder
public record SettlementCompletedExportRecord(
        /* Outbox 行标识 */
        Long outboxId,

        /* 结算完成事件标识 */
        String eventId,

        /* 结算批次标识 */
        Long settlementBatchId,

        /* 结算业务引用 */
        String settlementReference,

        /* 渠道结算批次标识 */
        String channelSettlementBatchId,

        /* 事件类型 */
        String eventType,

        /* 原始结算总额 */
        BigDecimal grossAmount,

        /* 总额币种 */
        String grossCurrency,

        /* 原始费用金额 */
        BigDecimal feeAmount,

        /* 费用币种 */
        String feeCurrency,

        /* 原始净额 */
        BigDecimal netAmount,

        /* 净额币种 */
        String netCurrency,

        /* Outbox 发布状态 */
        String publicationStatus,

        /* 结算完成时间 */
        Instant settledAt,

        /* Outbox 创建时间 */
        Instant createdAt,

        /* Outbox 发布时间 */
        Instant publishedAt,

        /* 原始链路标识 */
        String traceId
) { }
