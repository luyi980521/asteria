package io.asteria.reconciliation.infrastructure.persistence.repository;

import io.asteria.reconciliation.domain.entity.ReconciliationSourceRecord;
import io.asteria.reconciliation.domain.enums.ReconciliationSourceType;
import io.asteria.reconciliation.domain.repository.ReconciliationSourceRecordRepository;
import io.asteria.reconciliation.domain.valueobject.ReconciliationSourceRecordId;
import io.asteria.reconciliation.infrastructure.persistence.converter.ReconciliationSourceRecordPersistenceConverter;
import io.asteria.reconciliation.infrastructure.persistence.mapper.ReconciliationSourceRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 复用现有 Mapper + Converter 适配方式，事务边界由调用方负责。
 */
@Repository
@RequiredArgsConstructor
public class ReconciliationSourceRecordRepositoryImpl implements ReconciliationSourceRecordRepository {
    /** 数据库行映射。 */
    private final ReconciliationSourceRecordMapper mapper;
    /** 领域与行结构转换。 */
    private final ReconciliationSourceRecordPersistenceConverter converter;

    /**
     * 按主键读取，不额外加载其他对账对象。
     */
    @Override
    public Optional<ReconciliationSourceRecord> findById(ReconciliationSourceRecordId id) {
        return Optional.ofNullable(mapper.findById(id.value())).map(converter::toDomain);
    }

    /**
     * 按来源幂等键读取不可变快照。
     */
    @Override
    public Optional<ReconciliationSourceRecord> findBySource(ReconciliationSourceType sourceType, String sourceRecordId, Long sourceVersion) {
        return Optional.ofNullable(mapper.findBySource(sourceType.name(), sourceRecordId, sourceVersion))
                .map(converter::toDomain);
    }

    /**
     * 只插入新行，不执行可能覆盖历史证据的 upsert。
     */
    @Override
    public void save(ReconciliationSourceRecord domain) {
        mapper.insert(converter.toDataObject(domain));
    }
}
