package io.asteria.reconciliation.domain.repository;

import io.asteria.reconciliation.domain.entity.ReconciliationRun;
import io.asteria.reconciliation.domain.valueobject.ReconciliationRunId;

import java.util.Optional;

/**
 * ReconciliationRun 持久化端口；条件更新遵循现有版本锁约定。
 */
public interface ReconciliationRunRepository {
    /**
     * 按类型化主键读取，不存在时返回空值。
     */
    Optional<ReconciliationRun> findById(ReconciliationRunId id);

    /**
     * 插入新记录；主键冲突由数据库拒绝。
     */
    void save(ReconciliationRun domain);

    /**
     * 按原版本条件更新并在数据库递增；失败抛领域并发异常，成功后重新读取再进行下次更新。
     */
    void update(ReconciliationRun domain);
}
