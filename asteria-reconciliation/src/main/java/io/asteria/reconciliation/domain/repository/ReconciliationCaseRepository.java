package io.asteria.reconciliation.domain.repository;

import io.asteria.reconciliation.domain.entity.ReconciliationCase;
import io.asteria.reconciliation.domain.enums.ReconciliationDifferenceType;
import io.asteria.reconciliation.domain.valueobject.ReconciliationCaseId;

import java.util.Optional;

/**
 * ReconciliationCase 持久化端口；条件更新遵循现有版本锁约定。
 */
public interface ReconciliationCaseRepository {
    /**
     * 按类型化主键读取，不存在时返回空值。
     */
    Optional<ReconciliationCase> findById(ReconciliationCaseId id);

    /**
     * 读取同类活动工单，最终并发去重仍由部分唯一索引保证。
     */
    Optional<ReconciliationCase> findActive(String ruleCode, String businessKey, ReconciliationDifferenceType differenceType);

    /**
     * 插入新记录；重复活动键由数据库拒绝。
     */
    void save(ReconciliationCase domain);

    /**
     * 按原版本条件更新并在数据库递增；失败抛领域并发异常，成功后重新读取再进行下次更新。
     */
    void update(ReconciliationCase domain);
}
