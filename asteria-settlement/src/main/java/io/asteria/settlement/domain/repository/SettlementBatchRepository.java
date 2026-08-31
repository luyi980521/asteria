package io.asteria.settlement.domain.repository;

import io.asteria.settlement.domain.entity.SettlementBatch;
import io.asteria.settlement.domain.valueobject.SettlementBatchId;
import io.asteria.settlement.domain.valueobject.SettlementBatchReference;

import java.util.Optional;

/**
 * Settlement Batch 仓储。
 */
public interface SettlementBatchRepository {

    /**
     * 新增结算批次。
     */
    void insert(SettlementBatch settlementBatch);

    /**
     * 更新结算批次。
     */
    void update(SettlementBatch settlementBatch);

    /**
     * 根据 ID 查询结算批次。
     */
    Optional<SettlementBatch> findById(SettlementBatchId settlementBatchId);

    /**
     * 根据业务引用查询结算批次。
     */
    Optional<SettlementBatch> findByReference(SettlementBatchReference reference);
}