package io.asteria.reconciliation.infrastructure.persistence.repository;

import io.asteria.reconciliation.domain.repository.ReconciliationDeliveryBatchRepository;
import io.asteria.reconciliation.domain.entity.ReconciliationDeliveryBatch;
import io.asteria.reconciliation.domain.enums.ReconciliationFileBatchStatus;
import io.asteria.reconciliation.domain.valueobject.ReconciliationDeliveryBatchId;
import io.asteria.reconciliation.domain.error.ReconciliationErrorCode;
import io.asteria.common.domain.exception.BusinessException;
import io.asteria.reconciliation.domain.exception.ReconciliationDomainException;
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

    /**
     * 插入新批次，不覆盖已登记的交付。
     *
     * @param batch 待登记批次
     */
    @Override
    public void insert(ReconciliationDeliveryBatch batch) {
        mapper.insert(converter.toDataObject(batch));
    }

    /**
     * 保存实体当前处理数据，数据库刷新更新时间，不重做领域状态转换。
     *
     * @param batch 已变更的批次实体
     * @throws BusinessException 批次不存在或更新时间已过期
     */
    @Override
    public void update(ReconciliationDeliveryBatch batch) {
        if (mapper.updateEntity(converter.toDataObject(batch)) != 1) {
            throw new BusinessException(ReconciliationErrorCode.CONCURRENT_MODIFICATION);
        }
    }

    /**
     * 条件更新处理元数据，沿用对账 Run 的并发冲突报告方式。
     *
     * @param batch 更新后的批次
     * @param expectedStatus 更新前状态
     * @param expectedUpdatedAt 更新前时间
     * @throws ReconciliationDomainException 条件不匹配或批次不存在
     */
    @Override
    public void update(ReconciliationDeliveryBatch batch, ReconciliationFileBatchStatus expectedStatus, Instant expectedUpdatedAt) {
        if (!updateMetadata(batch, expectedStatus, expectedUpdatedAt)) {
            throw new ReconciliationDomainException(ReconciliationErrorCode.CONCURRENT_MODIFICATION);
        }
    }

    /**
     * 在数据库中一次完成预期状态和时间的比较及状态写入。
     *
     * @param id 批次标识
     * @param originalStatus 原状态
     * @param targetStatus 目标状态
     * @return 是否成功更新一行
     */
    @Override
    public boolean updateStatus(ReconciliationDeliveryBatchId id, ReconciliationFileBatchStatus originalStatus,
                                ReconciliationFileBatchStatus targetStatus) {
        return mapper.updateStatus(id.value(), originalStatus.name(), targetStatus.name()) == 1;
    }

    @Override
    public Optional<ReconciliationDeliveryBatch> findById(ReconciliationDeliveryBatchId id) {
        return Optional.ofNullable(mapper.findById(id.value())).map(converter::toDomain);
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
