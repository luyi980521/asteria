package io.asteria.reconciliation.domain.repository;

import io.asteria.reconciliation.domain.entity.ReconciliationSourceRecord;
import io.asteria.reconciliation.domain.enums.ReconciliationSourceType;
import io.asteria.reconciliation.domain.valueobject.ReconciliationSourceRecordId;

import java.util.Optional;

/**
 * ReconciliationSourceRecord 持久化端口；仅追加，禁止覆盖历史快照。
 */
public interface ReconciliationSourceRecordRepository {
    /**
     * 按类型化主键读取，不存在时返回空值。
     */
    Optional<ReconciliationSourceRecord> findById(ReconciliationSourceRecordId id);

    /**
     * 按来源幂等键读取不可变快照。
     */
    Optional<ReconciliationSourceRecord> findBySource(ReconciliationSourceType sourceType, String sourceRecordId, Long sourceVersion);

    /**
     * 插入新记录；来源幂等键冲突应读取已有快照，不覆盖原始数据。
     */
    void save(ReconciliationSourceRecord domain);
}
