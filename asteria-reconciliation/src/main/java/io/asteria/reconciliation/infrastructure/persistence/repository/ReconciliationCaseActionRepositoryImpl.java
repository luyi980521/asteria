package io.asteria.reconciliation.infrastructure.persistence.repository;

import io.asteria.reconciliation.domain.entity.ReconciliationCaseAction;
import io.asteria.reconciliation.domain.repository.ReconciliationCaseActionRepository;
import io.asteria.reconciliation.domain.valueobject.ReconciliationCaseActionId;
import io.asteria.reconciliation.infrastructure.persistence.converter.ReconciliationCaseActionPersistenceConverter;
import io.asteria.reconciliation.infrastructure.persistence.mapper.ReconciliationCaseActionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 复用现有 Mapper + Converter 适配方式，事务边界由调用方负责。
 */
@Repository
@RequiredArgsConstructor
public class ReconciliationCaseActionRepositoryImpl implements ReconciliationCaseActionRepository {
    /** 数据库行映射。 */
    private final ReconciliationCaseActionMapper mapper;
    /** 领域与行结构转换。 */
    private final ReconciliationCaseActionPersistenceConverter converter;

    /**
     * 按主键读取，不额外加载其他对账对象。
     */
    @Override
    public Optional<ReconciliationCaseAction> findById(ReconciliationCaseActionId id) {
        return Optional.ofNullable(mapper.findById(id.value())).map(converter::toDomain);
    }

    /**
     * 只插入新行，不执行可能覆盖历史证据的 upsert。
     */
    @Override
    public void save(ReconciliationCaseAction domain) {
        mapper.insert(converter.toDataObject(domain));
    }
}
