package io.asteria.reconciliation.infrastructure.persistence.repository;

import io.asteria.reconciliation.domain.entity.ReconciliationCase;
import io.asteria.reconciliation.domain.enums.ReconciliationDifferenceType;
import io.asteria.reconciliation.domain.error.ReconciliationErrorCode;
import io.asteria.reconciliation.domain.exception.ReconciliationDomainException;
import io.asteria.reconciliation.domain.repository.ReconciliationCaseRepository;
import io.asteria.reconciliation.domain.valueobject.ReconciliationCaseId;
import io.asteria.reconciliation.infrastructure.persistence.converter.ReconciliationCasePersistenceConverter;
import io.asteria.reconciliation.infrastructure.persistence.mapper.ReconciliationCaseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 复用现有 Mapper + Converter 适配方式，事务边界由调用方负责。
 */
@Repository
@RequiredArgsConstructor
public class ReconciliationCaseRepositoryImpl implements ReconciliationCaseRepository {
    /** 数据库行映射。 */
    private final ReconciliationCaseMapper mapper;
    /** 领域与行结构转换。 */
    private final ReconciliationCasePersistenceConverter converter;

    /**
     * 按主键读取，不额外加载其他对账对象。
     */
    @Override
    public Optional<ReconciliationCase> findById(ReconciliationCaseId id) {
        return Optional.ofNullable(mapper.findById(id.value())).map(converter::toDomain);
    }

    /**
     * 读取同类活动工单，最终并发去重仍由部分唯一索引保证。
     */
    @Override
    public Optional<ReconciliationCase> findActive(String ruleCode, String businessKey, ReconciliationDifferenceType differenceType) {
        return Optional.ofNullable(mapper.findActive(ruleCode, businessKey, differenceType.name()))
                .map(converter::toDomain);
    }

    /**
     * 只插入新行，不执行可能覆盖历史证据的 upsert。
     */
    @Override
    public void save(ReconciliationCase domain) {
        mapper.insert(converter.toDataObject(domain));
    }

    /**
     * 版本过期或记录不存在时拒绝更新，沿用 Balance 的条件更新语义。
     */
    @Override
    public void update(ReconciliationCase domain) {
        if (mapper.updateWithVersion(converter.toDataObject(domain)) != 1) {
            throw new ReconciliationDomainException(ReconciliationErrorCode.CONCURRENT_MODIFICATION);
        }
    }
}
