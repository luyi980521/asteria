package io.asteria.payment.application.service.impl;

import io.asteria.payment.application.service.OutboxTransactionService;
import io.asteria.payment.domain.repository.OutboxEventRepository;
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
public class OutboxTransactionServiceImpl implements OutboxTransactionService {

    private final OutboxEventRepository outboxEventRepository;

    /**
     * 标记 Outbox 事件已发布
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markPublished(Long id, Instant publishedAt) {
        outboxEventRepository.markPublished(id, publishedAt);
    }
}