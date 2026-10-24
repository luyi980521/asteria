package io.asteria.reconciliation.domain.repository;

import io.asteria.reconciliation.domain.entity.ReconciliationRule;
import io.asteria.reconciliation.domain.valueobject.ReconciliationRuleId;

import java.util.Optional;

/**
 * ReconciliationRule 持久化端口；条件更新遵循现有版本锁约定。
 */
public interface ReconciliationRuleRepository {
    /**
     * 按类型化主键读取，不存在时返回空值。
     */
    Optional<ReconciliationRule> findById(ReconciliationRuleId id);

    /**
     * 按稳定规则编码读取当前版本。
     */
    Optional<ReconciliationRule> findByRuleCode(String ruleCode);

    /**
     * 插入新记录；主键冲突由数据库拒绝。
     */
    void save(ReconciliationRule domain);

    /**
     * 按原版本条件更新并在数据库递增；失败抛领域并发异常，成功后重新读取再进行下次更新。
     */
    void update(ReconciliationRule domain);
}
