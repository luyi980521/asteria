package io.asteria.reconciliation.domain.repository;

import io.asteria.reconciliation.domain.entity.ReconciliationCaseAction;
import io.asteria.reconciliation.domain.valueobject.ReconciliationCaseActionId;

import java.util.Optional;

/**
 * ReconciliationCaseAction 持久化端口；仅追加，禁止覆盖历史快照。
 */
public interface ReconciliationCaseActionRepository {
    /**
     * 按类型化主键读取，不存在时返回空值。
     */
    Optional<ReconciliationCaseAction> findById(ReconciliationCaseActionId id);

    /**
     * 插入新记录；必须与 Case 变更在调用方同一本地事务内执行。
     */
    void save(ReconciliationCaseAction domain);
}
