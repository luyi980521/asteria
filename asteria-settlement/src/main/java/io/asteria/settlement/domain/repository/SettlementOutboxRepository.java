package io.asteria.settlement.domain.repository;

import io.asteria.settlement.domain.valueobject.SettlementOutboxEvent;

import java.time.Instant;
import java.util.List;

/**
 * 结算 Outbox 事件仓储
 */
public interface SettlementOutboxRepository {

    /**
     * 插入 Outbox 事件
     */
    void insert(SettlementOutboxEvent event);

    /**
     * 查询待发布事件
     */
    List<SettlementOutboxEvent> findPending(int limit);

    /**
     * 标记为已发布
     */
    void markPublished(Long id, Instant publishedAt);
}