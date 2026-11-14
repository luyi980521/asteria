package io.asteria.reconciliation.domain.repository;

import io.asteria.reconciliation.domain.valueobject.ReconciliationDeliveryBatch;
import io.asteria.reconciliation.domain.enums.ReconciliationFileBatchStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * ReconciliationDeliveryBatch 持久化端口，事务边界及业务状态策略由调用方负责。
 */
public interface ReconciliationDeliveryBatchRepository {
    /**
     * 按主键读取。
     */
    Optional<ReconciliationDeliveryBatch> findById(Long id);
    /**
     * 按来源、对象键及版本读取已登记描述；冲突后的内容比较由调用方负责。
     */
    Optional<ReconciliationDeliveryBatch> findByDelivery(String sourceSystem, String objectKey, Long fileVersion);
    /**
     * 按来源和业务日期读取有界 ID 升序页；limit 为 1..1000。
     */
    List<ReconciliationDeliveryBatch> findBySourceAndBusinessDate(String sourceSystem, LocalDate businessDate, long afterId, int limit);
    /**
     * 按状态读取有界 ID 升序页；首屏 afterId 为 0，limit 为 1..1000。
     */
    List<ReconciliationDeliveryBatch> findByStatus(ReconciliationFileBatchStatus status, long afterId, int limit);
    /**
     * 原子登记，交付唯一键存在时返回 false，不覆盖已有描述。
     */
    boolean saveIfAbsent(ReconciliationDeliveryBatch batch);
    /**
     * 按预期状态与更新时间更新实际数量、状态、原因及处理时间，不修改来源身份与文件描述。
     */
    boolean updateMetadata(ReconciliationDeliveryBatch batch, ReconciliationFileBatchStatus expectedStatus, Instant expectedUpdatedAt);

}