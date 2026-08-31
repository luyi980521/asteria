package io.asteria.settlement.application.service;

import io.asteria.settlement.application.command.CreateSettlementBatchCommand;
import io.asteria.settlement.domain.entity.SettlementBatch;
import io.asteria.settlement.domain.valueobject.SettlementBatchId;

/**
 * Settlement Batch 应用服务。
 */
public interface SettlementBatchApplicationService {

    /**
     * 创建结算批次。
     */
    SettlementBatch create(CreateSettlementBatchCommand command);

    /**
     * 提交结算批次。
     */
    void submit(SettlementBatchId settlementBatchId);
}