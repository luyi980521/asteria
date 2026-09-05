package io.asteria.payment.application.service;

import java.time.Instant;

/**
 * Outbox 功能接口定义
 * */
public interface PaymentOutboxTransactionService {

    void markPublished(Long id, Instant publishedAt);
}
