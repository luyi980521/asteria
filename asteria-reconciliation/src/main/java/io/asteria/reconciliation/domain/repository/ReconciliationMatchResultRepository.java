package io.asteria.reconciliation.domain.repository;

import io.asteria.reconciliation.domain.entity.ReconciliationMatchResult;
import io.asteria.reconciliation.domain.valueobject.ReconciliationMatchResultId;
import io.asteria.reconciliation.domain.valueobject.ReconciliationRunId;

import java.util.Optional;

/**
 * ReconciliationMatchResult 持久化端口；仅追加，禁止覆盖历史快照。
 */
public interface ReconciliationMatchResultRepository {
    /**
     * 按类型化主键读取，不存在时返回空值。
     */
    Optional<ReconciliationMatchResult> findById(ReconciliationMatchResultId id);

    /**
     * 按运行、规则与业务键读取唯一结论。
     */
    Optional<ReconciliationMatchResult> findByRunAndBusinessKey(ReconciliationRunId runId, String ruleCode, String businessKey);

    /**
     * 插入新记录；重复运行规则业务键由数据库拒绝。
     */
    void save(ReconciliationMatchResult domain);
}
