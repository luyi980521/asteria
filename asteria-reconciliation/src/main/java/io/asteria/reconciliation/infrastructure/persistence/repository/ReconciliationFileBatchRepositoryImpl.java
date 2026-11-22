package io.asteria.reconciliation.infrastructure.persistence.repository;

import io.asteria.reconciliation.domain.repository.ReconciliationFileBatchRepository;
import io.asteria.reconciliation.domain.entity.ReconciliationFileBatch;
import io.asteria.reconciliation.domain.enums.ReconciliationFileBatchStatus;
import io.asteria.reconciliation.infrastructure.persistence.converter.ReconciliationFileBatchPersistenceConverter;
import io.asteria.reconciliation.infrastructure.persistence.mapper.ReconciliationFileBatchMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import io.asteria.reconciliation.domain.valueobject.ReconciliationFileBatchId;
import io.asteria.reconciliation.domain.valueobject.ReconciliationDeliveryBatchId;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * ReconciliationFileBatch 数据访问适配，不执行业务完整性判断。
 */
@Repository
@RequiredArgsConstructor
public class ReconciliationFileBatchRepositoryImpl implements ReconciliationFileBatchRepository {
    /** 数据行访问 */
    private final ReconciliationFileBatchMapper mapper;
    /** 显式快照转换 */
    private final ReconciliationFileBatchPersistenceConverter converter;

    @Override
    public Optional<ReconciliationFileBatch> findById(ReconciliationFileBatchId id) {
        return Optional.ofNullable(mapper.findById(id.value())).map(converter::toDomain);
    }
    @Override
    public Optional<ReconciliationFileBatch> findByDelivery(String sourceSystem, String objectKey, Long fileVersion) {
        return Optional.ofNullable(mapper.findByDelivery(sourceSystem, objectKey, fileVersion)).map(converter::toDomain);
    }
    @Override
    public List<ReconciliationFileBatch> findBySourceAndBusinessDate(String sourceSystem, LocalDate businessDate, long afterId, int limit) {
        checkPage(afterId, limit);
        return mapper.findBySourceAndBusinessDate(sourceSystem, businessDate, afterId, limit).stream().map(converter::toDomain).toList();
    }
    @Override
    public List<ReconciliationFileBatch> findByStatus(ReconciliationFileBatchStatus status, long afterId, int limit) {
        checkPage(afterId, limit);
        return mapper.findByStatus(status.name(), afterId, limit).stream().map(converter::toDomain).toList();
    }
    @Override
    public boolean saveIfAbsent(ReconciliationFileBatch batch) {
        return mapper.insertIfAbsent(converter.toDataObject(batch)) == 1;
    }
    @Override
    public boolean updateMetadata(ReconciliationFileBatch batch, ReconciliationFileBatchStatus expectedStatus, Instant expectedUpdatedAt) {
        return mapper.updateMetadata(converter.toDataObject(batch), expectedStatus.name(), expectedUpdatedAt) == 1;
    }
    @Override
    public List<ReconciliationFileBatch> findByBatchId(ReconciliationDeliveryBatchId batchId, long afterId, int limit) {
        checkPage(afterId, limit);
        return mapper.findByBatchId(batchId.value(), afterId, limit).stream().map(converter::toDomain).toList();
    }
    @Override
    public boolean updateImportStatus(ReconciliationFileBatchId fileId, String expectedStatus, String status, Instant completedAt, String failureReason) {
        return mapper.updateImportStatus(fileId.value(), expectedStatus, status, completedAt, failureReason) == 1;
    }
    private void checkPage(long afterId, int limit) {
        if (afterId < 0 || limit < 1 || limit > 1000) throw new IllegalArgumentException("Invalid batch page");
    }
}
