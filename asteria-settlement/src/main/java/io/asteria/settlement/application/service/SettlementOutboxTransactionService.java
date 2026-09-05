package io.asteria.settlement.application.service;

import java.time.Instant;

/**
 * Outbox 功能接口定义
 * */
public interface SettlementOutboxTransactionService {

    void markPublished(Long id, Instant publishedAt);
}
