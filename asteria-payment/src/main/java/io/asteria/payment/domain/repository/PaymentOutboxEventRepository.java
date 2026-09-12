package io.asteria.payment.domain.repository;

import io.asteria.payment.domain.valueobject.PaymentOutboxEvent;

import java.time.Instant;
import java.util.List;

/**
 * 支付 Outbox 事件仓储
 */
public interface PaymentOutboxEventRepository {

    /**
     * 插入 Outbox 事件
     */
    void insert(PaymentOutboxEvent event);

    /**
     * 查询待发布事件
     */
    List<PaymentOutboxEvent> findPending(int limit);

    /** 按聚合 ID 批量查询事件，不限制发布状态。 */
    List<PaymentOutboxEvent> findByAggregateIds(List<String> aggregateIds);

    /**
     * 标记为已发布
     */
    void markPublished(Long id, Instant publishedAt);
}
