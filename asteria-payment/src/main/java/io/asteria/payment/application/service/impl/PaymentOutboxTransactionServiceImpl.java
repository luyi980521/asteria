package io.asteria.payment.application.service.impl;

import io.asteria.payment.application.service.PaymentOutboxTransactionService;
import io.asteria.payment.domain.repository.PaymentOutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Outbox 功能接口定义实现类
 * */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentOutboxTransactionServiceImpl implements PaymentOutboxTransactionService {

    private final PaymentOutboxEventRepository paymentOutboxEventRepository;

    /**
     * 标记 Outbox 事件已发布
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markPublished(Long id, Instant publishedAt) {
        paymentOutboxEventRepository.markPublished(id, publishedAt);
    }
}