package io.asteria.settlement.application.service;

import io.asteria.settlement.application.port.channel.SettlementBatchResult;
import io.asteria.settlement.domain.entity.SettlementBatch;
import io.asteria.settlement.domain.valueobject.SettlementBatchId;

/**
 * 结算批次事务操作功能接口定义
 * */
public interface SettlementBatchTransactionService {

    /**
     * 开始处理结算批次，CREATED -> PROCESSING
     */
    SettlementBatch startProcessing(SettlementBatchId settlementBatchId);

    /**
     * 结算已受理，PROCESSING -> ACCEPTED
     */
    void accept(SettlementBatchId settlementBatchId, SettlementBatchResult result);

    /**
     * 结算失败，PROCESSING -> FAILED
     */
    void fail(SettlementBatchId settlementBatchId, SettlementBatchResult result);

    /**
     * 结果未知继续等待，状态不推进
     */
    void markSubmitUnknown(SettlementBatchId settlementBatchId);
}
