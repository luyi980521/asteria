package io.asteria.reconciliation.infrastructure.persistence.repository;

import io.asteria.reconciliation.domain.entity.ReconciliationMatchResult;
import io.asteria.reconciliation.domain.repository.ReconciliationMatchResultRepository;
import io.asteria.reconciliation.domain.valueobject.ReconciliationMatchResultId;
import io.asteria.reconciliation.domain.valueobject.ReconciliationRunId;
import io.asteria.reconciliation.infrastructure.persistence.converter.ReconciliationMatchResultPersistenceConverter;
import io.asteria.reconciliation.infrastructure.persistence.mapper.ReconciliationMatchResultMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 复用现有 Mapper + Converter 适配方式，事务边界由调用方负责。
 */
@Repository
@RequiredArgsConstructor
public class ReconciliationMatchResultRepositoryImpl implements ReconciliationMatchResultRepository {
    /** 数据库行映射。 */
    private final ReconciliationMatchResultMapper mapper;
    /** 领域与行结构转换。 */
    private final ReconciliationMatchResultPersistenceConverter converter;

    /**
     * 按主键读取，不额外加载其他对账对象。
     */
    @Override
    public Optional<ReconciliationMatchResult> findById(ReconciliationMatchResultId id) {
        return Optional.ofNullable(mapper.findById(id.value())).map(converter::toDomain);
    }

    /**
     * 按运行、规则与业务键读取唯一结论。
     */
    @Override
    public Optional<ReconciliationMatchResult> findByRunAndBusinessKey(ReconciliationRunId runId, String ruleCode, String businessKey) {
        return Optional.ofNullable(mapper.findByRunAndBusinessKey(runId.value(), ruleCode, businessKey))
                .map(converter::toDomain);
    }

    /**
     * 只插入新行，不执行可能覆盖历史证据的 upsert。
     */
    @Override
    public void save(ReconciliationMatchResult domain) {
        mapper.insert(converter.toDataObject(domain));
    }
}
