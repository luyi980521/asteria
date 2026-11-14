package io.asteria.reconciliation.application.model;

import lombok.Builder;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * 支付捕获事件的文件快照；eventId 标识每次捕获，paymentId 只标识聚合。金额拆列无损传输，不执行 Money 业务校验。
 */
@Builder
public record PaymentCaptureExportRecord(
        /** Outbox 行主键。 */ Long outboxId,
        /** 捕获事件唯一标识，不以 Payment ID 替代。 */ String eventId,
        /** 支付聚合主键。 */ Long paymentId,
        /** 支付业务引用类型。 */ String referenceType,
        /** 支付业务引用标识。 */ String referenceId,
        /** 捕获事件类型。 */ String eventType,
        /** 捕获金额，来自事件 Money.amount。 */ BigDecimal amount,
        /** 币种，来自事件 Money.currency。 */ String currency,
        /** Outbox 发布状态，不代表支付状态。 */ String publicationStatus,
        /** 捕获业务时间。 */ Instant capturedAt,
        /** Outbox 创建时间。 */ Instant createdAt,
        /** Outbox 发布时间，可空。 */ Instant publishedAt,
        /** 链路标识，可空。 */ String traceId
) { }
