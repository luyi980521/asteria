package io.asteria.settlement.domain.valueobject;

import io.asteria.settlement.domain.enums.SettlementOutboxEventStatus;
import io.asteria.settlement.domain.enums.SettlementOutboxEventType;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

/**
 * 结算 Outbox 事件
 */
@Getter
@Builder
public class SettlementOutboxEvent {

    /** Outbox 记录 ID */
    private final Long id;

    /** 业务事件唯一 ID */
    private final String eventId;

    /** 聚合类型 */
    private final String aggregateType;

    /** 聚合 ID */
    private final String aggregateId;

    /** 事件类型 */
    private final SettlementOutboxEventType eventType;

    /** 消息内容 */
    private final String payload;


    /** 发布状态 */
    private SettlementOutboxEventStatus status;

    /** 创建时间 */
    private final Instant createdAt;

    /** 发布时间 */
    private Instant publishedAt;

    /** Persisted correlation ID, independent of the message payload. */
    private final String traceId;

    public SettlementOutboxEvent(Long id, String eventId, String aggregateType, String aggregateId,
                                 SettlementOutboxEventType eventType, String payload, SettlementOutboxEventStatus status,
                                 Instant createdAt, Instant publishedAt, String traceId) {
        this.id = id;
        this.eventId = eventId;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payload = payload;
        this.traceId = traceId;
        this.status = status;
        this.createdAt = createdAt;
        this.publishedAt = publishedAt;
    }
}
