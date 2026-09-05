package io.asteria.settlement.application.service.impl;

import io.asteria.settlement.application.service.SettlementOutboxTransactionService;
import io.asteria.settlement.domain.repository.SettlementOutboxRepository;
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
public class SettlementOutboxTransactionServiceImpl implements SettlementOutboxTransactionService {

    private final SettlementOutboxRepository settlementOutboxRepository;

    /**
     * 标记 Outbox 事件已发布
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markPublished(Long id, Instant publishedAt) {
        settlementOutboxRepository.markPublished(id, publishedAt);
    }
}