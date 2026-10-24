package io.asteria.reconciliation.infrastructure.persistence.repository;

import io.asteria.reconciliation.domain.entity.ReconciliationRule;
import io.asteria.reconciliation.domain.error.ReconciliationErrorCode;
import io.asteria.reconciliation.domain.exception.ReconciliationDomainException;
import io.asteria.reconciliation.domain.repository.ReconciliationRuleRepository;
import io.asteria.reconciliation.domain.valueobject.ReconciliationRuleId;
import io.asteria.reconciliation.infrastructure.persistence.converter.ReconciliationRulePersistenceConverter;
import io.asteria.reconciliation.infrastructure.persistence.mapper.ReconciliationRuleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 复用现有 Mapper + Converter 适配方式，事务边界由调用方负责。
 */
@Repository
@RequiredArgsConstructor
public class ReconciliationRuleRepositoryImpl implements ReconciliationRuleRepository {
    /** 数据库行映射。 */
    private final ReconciliationRuleMapper mapper;
    /** 领域与行结构转换。 */
    private final ReconciliationRulePersistenceConverter converter;

    /**
     * 按主键读取，不额外加载其他对账对象。
     */
    @Override
    public Optional<ReconciliationRule> findById(ReconciliationRuleId id) {
        return Optional.ofNullable(mapper.findById(id.value())).map(converter::toDomain);
    }

    /**
     * 按稳定规则编码读取当前版本。
     */
    @Override
    public Optional<ReconciliationRule> findByRuleCode(String ruleCode) {
        return Optional.ofNullable(mapper.findByRuleCode(ruleCode))
                .map(converter::toDomain);
    }

    /**
     * 只插入新行，不执行可能覆盖历史证据的 upsert。
     */
    @Override
    public void save(ReconciliationRule domain) {
        mapper.insert(converter.toDataObject(domain));
    }

    /**
     * 版本过期或记录不存在时拒绝更新，沿用 Balance 的条件更新语义。
     */
    @Override
    public void update(ReconciliationRule domain) {
        if (mapper.updateWithVersion(converter.toDataObject(domain)) != 1) {
            throw new ReconciliationDomainException(ReconciliationErrorCode.CONCURRENT_MODIFICATION);
        }
    }
}
