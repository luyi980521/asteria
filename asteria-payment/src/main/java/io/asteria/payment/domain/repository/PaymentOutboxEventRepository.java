package io.asteria.payment.domain.repository;

import io.asteria.payment.domain.valueobject.PaymentOutboxEvent;

import java.time.Instant;
import java.util.List;

/**
 * 支付 Outbox 事件仓储
 */
public interface PaymentOutboxEventRepository {

    /**
     * 按 payload.capturedAt 的 [start, end) 查询已完成捕获事实，不依赖发布状态。
     * afterId 为上一页最后的 Outbox ID，首屏用 0；按 ID 升序，limit 为 1..10000。
     * 导出方负责在一致性快照或完成水位下扫描，ID 本身不保证提交顺序。
     */
    List<PaymentOutboxEvent> findCapturedInWindow(Instant start, Instant end, long afterId, int limit);

    /**
     * 插入 Outbox 事件
     */
    void insert(PaymentOutboxEvent event);

    /**
     * 查询待发布事件
     */
    List<PaymentOutboxEvent> findPending(int limit);

    /**
     * 按聚合 ID 批量查询事件，不限制发布状态。
     */
    List<PaymentOutboxEvent> findByAggregateIds(List<String> aggregateIds);

    /**
     * 标记为已发布
     */
    void markPublished(Long id, Instant publishedAt);
}
