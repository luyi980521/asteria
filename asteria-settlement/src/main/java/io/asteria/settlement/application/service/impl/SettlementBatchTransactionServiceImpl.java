package io.asteria.settlement.application.service.impl;

import io.asteria.settlement.application.port.channel.SettlementBatchResult;
import io.asteria.settlement.application.service.SettlementBatchTransactionService;
import io.asteria.settlement.domain.entity.SettlementBatch;
import io.asteria.settlement.domain.error.SettlementErrorCode;
import io.asteria.settlement.domain.exception.SettlementDomainException;
import io.asteria.settlement.domain.repository.SettlementBatchRepository;
import io.asteria.settlement.domain.valueobject.SettlementBatchId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * 结算批次事务操作功能接口定义实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SettlementBatchTransactionServiceImpl implements SettlementBatchTransactionService {

    private final SettlementBatchRepository settlementBatchRepository;

    /**
     * 开始处理结算批次，CREATED -> PROCESSING
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SettlementBatch startProcessing(SettlementBatchId settlementBatchId) {
        SettlementBatch settlementBatch = findById(settlementBatchId);
        settlementBatch.startProcessing(Instant.now());
        settlementBatchRepository.update(settlementBatch);
        return settlementBatch;
    }

    /**
     * 结算已受理，PROCESSING -> ACCEPTED
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void accept(SettlementBatchId settlementBatchId, SettlementBatchResult result) {
        SettlementBatch settlementBatch = findById(settlementBatchId);
        settlementBatch.accept(result.channelSettlementBatchId(),
                result.acceptedAt() != null ? result.acceptedAt() : Instant.now());
        settlementBatchRepository.update(settlementBatch);
    }

    /**
     * 结算失败，PROCESSING -> FAILED
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void fail(SettlementBatchId settlementBatchId, SettlementBatchResult result) {
        SettlementBatch settlementBatch = findById(settlementBatchId);
        settlementBatch.fail(Instant.now());
        settlementBatchRepository.update(settlementBatch);
        log.warn("Settlement batch failed, settlementBatchId: {}, failureCode: {}, failureMessage: {}",
                settlementBatchId.value(), result.failureCode(), result.failureMessage());
    }

    /**
     * 结果未知继续等待，状态不推进
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markSubmitUnknown(SettlementBatchId settlementBatchId) {
        SettlementBatch settlementBatch = findById(settlementBatchId);
        settlementBatch.markSubmitUnknown();
        settlementBatchRepository.update(settlementBatch);
        log.warn("Settlement batch submit result unknown, settlementBatchId: {}, status: {}",
                settlementBatchId.value(), settlementBatch.getStatus());
    }

    private SettlementBatch findById(SettlementBatchId settlementBatchId) {
        return settlementBatchRepository.findById(settlementBatchId)
                .orElseThrow(() -> {
                    log.warn("The settlement batch doesn't exist: {}", settlementBatchId.value());
                    return new SettlementDomainException(SettlementErrorCode.SETTLEMENT_BATCH_NOT_FOUND);
                });
    }

    /**
     * 标记结算完成，ACCEPTED -> SETTLED
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void settle(SettlementBatchId settlementBatchId, Instant settledAt) {
        SettlementBatch settlementBatch = settlementBatchRepository.findById(settlementBatchId)
                .orElseThrow(() -> new SettlementDomainException(
                        SettlementErrorCode.SETTLEMENT_BATCH_NOT_FOUND));

        settlementBatch.settle(settledAt);
        settlementBatchRepository.update(settlementBatch);
    }
}
