package io.asteria.payment.domain.repository;

import io.asteria.payment.domain.valueobject.OutboxEvent;

import java.time.Instant;
import java.util.List;

/**
 * 支付 Outbox 事件仓储
 */
public interface PaymentOutboxEventRepository {

    /**
     * 插入 Outbox 事件
     */
    void insert(OutboxEvent event);

    /**
     * 查询待发布事件
     */
    List<OutboxEvent> findPending(int limit);

    /**
     * 标记为已发布
     */
    void markPublished(Long id, Instant publishedAt);
}