package io.asteria.reconciliation.infrastructure.persistence.repository;

import io.asteria.reconciliation.domain.repository.ReconciliationDeliveryBatchRepository;
import io.asteria.reconciliation.domain.valueobject.ReconciliationDeliveryBatch;
import io.asteria.reconciliation.domain.enums.ReconciliationFileBatchStatus;
import io.asteria.reconciliation.infrastructure.persistence.converter.ReconciliationDeliveryBatchPersistenceConverter;
import io.asteria.reconciliation.infrastructure.persistence.mapper.ReconciliationDeliveryBatchMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * ReconciliationDeliveryBatch 数据访问适配，不执行业务完整性判断。
 */
@Repository
@RequiredArgsConstructor
public class ReconciliationDeliveryBatchRepositoryImpl implements ReconciliationDeliveryBatchRepository {
    /** 数据行访问 */
    private final ReconciliationDeliveryBatchMapper mapper;
    /** 显式快照转换 */
    private final ReconciliationDeliveryBatchPersistenceConverter converter;

    @Override
    public Optional<ReconciliationDeliveryBatch> findById(Long id) {
        return Optional.ofNullable(mapper.findById(id)).map(converter::toDomain);
    }
    @Override
    public Optional<ReconciliationDeliveryBatch> findByDelivery(String sourceSystem, String objectKey, Long fileVersion) {
        return Optional.ofNullable(mapper.findByDelivery(sourceSystem, objectKey, fileVersion)).map(converter::toDomain);
    }
    @Override
    public List<ReconciliationDeliveryBatch> findBySourceAndBusinessDate(String sourceSystem, LocalDate businessDate, long afterId, int limit) {
        checkPage(afterId, limit);
        return mapper.findBySourceAndBusinessDate(sourceSystem, businessDate, afterId, limit).stream().map(converter::toDomain).toList();
    }
    @Override
    public List<ReconciliationDeliveryBatch> findByStatus(ReconciliationFileBatchStatus status, long afterId, int limit) {
        checkPage(afterId, limit);
        return mapper.findByStatus(status.name(), afterId, limit).stream().map(converter::toDomain).toList();
    }
    @Override
    public boolean saveIfAbsent(ReconciliationDeliveryBatch batch) {
        return mapper.insertIfAbsent(converter.toDataObject(batch)) == 1;
    }
    @Override
    public boolean updateMetadata(ReconciliationDeliveryBatch batch, ReconciliationFileBatchStatus expectedStatus, Instant expectedUpdatedAt) {
        return mapper.updateMetadata(converter.toDataObject(batch), expectedStatus.name(), expectedUpdatedAt) == 1;
    }

    private void checkPage(long afterId, int limit) {
        if (afterId < 0 || limit < 1 || limit > 1000) throw new IllegalArgumentException("Invalid batch page");
    }
}