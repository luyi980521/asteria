package io.asteria.reconciliation.infrastructure.persistence.repository;

import io.asteria.reconciliation.domain.entity.ReconciliationRun;
import io.asteria.reconciliation.domain.error.ReconciliationErrorCode;
import io.asteria.reconciliation.domain.exception.ReconciliationDomainException;
import io.asteria.reconciliation.domain.repository.ReconciliationRunRepository;
import io.asteria.reconciliation.domain.valueobject.ReconciliationRunId;
import io.asteria.reconciliation.infrastructure.persistence.converter.ReconciliationRunPersistenceConverter;
import io.asteria.reconciliation.infrastructure.persistence.mapper.ReconciliationRunMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 复用现有 Mapper + Converter 适配方式，事务边界由调用方负责。
 */
@Repository
@RequiredArgsConstructor
public class ReconciliationRunRepositoryImpl implements ReconciliationRunRepository {
    /** 数据库行映射。 */
    private final ReconciliationRunMapper mapper;
    /** 领域与行结构转换。 */
    private final ReconciliationRunPersistenceConverter converter;

    /**
     * 按主键读取，不额外加载其他对账对象。
     */
    @Override
    public Optional<ReconciliationRun> findById(ReconciliationRunId id) {
        return Optional.ofNullable(mapper.findById(id.value())).map(converter::toDomain);
    }

    /**
     * 只插入新行，不执行可能覆盖历史证据的 upsert。
     */
    @Override
    public void save(ReconciliationRun domain) {
        mapper.insert(converter.toDataObject(domain));
    }

    /**
     * 版本过期或记录不存在时拒绝更新，沿用 Balance 的条件更新语义。
     */
    @Override
    public void update(ReconciliationRun domain) {
        if (mapper.updateWithVersion(converter.toDataObject(domain)) != 1) {
            throw new ReconciliationDomainException(ReconciliationErrorCode.CONCURRENT_MODIFICATION);
        }
    }
}
