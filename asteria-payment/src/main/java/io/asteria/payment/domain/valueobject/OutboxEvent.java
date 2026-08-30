package io.asteria.payment.domain.valueobject;

import io.asteria.payment.domain.enums.OutboxEventStatus;
import io.asteria.payment.domain.enums.OutboxEventType;
import lombok.Getter;

import java.time.Instant;

/**
 * Outbox 事件
 */
@Getter
public class OutboxEvent {

    /** Outbox 记录 ID */
    private final Long id;

    /** 业务事件唯一 ID */
    private final String eventId;

    /** 聚合类型 */
    private final String aggregateType;

    /** 聚合 ID */
    private final String aggregateId;

    /** 事件类型 */
    private final OutboxEventType eventType;

    /** 消息内容 */
    private final String payload;

    /** 发布状态 */
    private OutboxEventStatus status;

    /** 创建时间 */
    private final Instant createdAt;

    /** 发布时间 */
    private Instant publishedAt;

    public OutboxEvent(Long id, String eventId, String aggregateType, String aggregateId,
                       OutboxEventType eventType, String payload, OutboxEventStatus status,
                       Instant createdAt, Instant publishedAt) {
        this.id = id;
        this.eventId = eventId;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payload = payload;
        this.status = status;
        this.createdAt = createdAt;
        this.publishedAt = publishedAt;
    }
}