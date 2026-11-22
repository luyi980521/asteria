package io.asteria.reconciliation.application.service;

import io.asteria.reconciliation.domain.valueobject.ReconciliationDeliveryBatchId;

/**
 * 对账多文件交付批次头功能接口定义
 */
public interface ReconciliationDeliveryBatchApplicationService {

    /**
     * 验证交付批次
     * @param batchId 批次 id
     */
    void validateDeliveryBatch(ReconciliationDeliveryBatchId batchId);
}