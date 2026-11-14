package io.asteria.reconciliation.domain.repository;

import io.asteria.reconciliation.domain.entity.ReconciliationSourceRecord;
import io.asteria.reconciliation.domain.enums.ReconciliationSourceType;
import io.asteria.reconciliation.domain.valueobject.ReconciliationSourceRecordId;

import java.util.Optional;
import java.util.List;

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

    /**
     * 按来源三元组原子插入；已存在返回 false，主键等其他冲突仍抛持久化异常。
     */
    boolean saveIfAbsent(ReconciliationSourceRecord domain);

    /**
     * 分块批量插入并忽略已存在的来源三元组，返回新增行数；事务边界由调用方管理。
     */
    int saveAllIfAbsent(List<ReconciliationSourceRecord> records);
}
