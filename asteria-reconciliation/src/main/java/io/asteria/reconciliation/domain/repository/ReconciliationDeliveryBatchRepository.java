package io.asteria.reconciliation.domain.repository;

import io.asteria.reconciliation.domain.enums.ReconciliationFileBatchStatus;
import io.asteria.reconciliation.domain.entity.ReconciliationDeliveryBatch;
import io.asteria.reconciliation.domain.valueobject.ReconciliationDeliveryBatchId;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * ReconciliationDeliveryBatch 持久化端口，事务边界及业务状态策略由调用方负责。
 */
public interface ReconciliationDeliveryBatchRepository {
    /**
     * 插入新批次，主键或交付唯一键冲突由数据库异常报告。
     *
     * @param batch 待登记批次
     */
    void insert(ReconciliationDeliveryBatch batch);

    /**
     * 保存实体当前的处理元数据，不执行业务状态推进。
     *
     * @param batch 已由领域方法变更的实体，其更新时间用于并发检查
     * @throws io.asteria.common.domain.exception.BusinessException 批次不存在或已被并发更新
     */
    void update(ReconciliationDeliveryBatch batch);

    /**
     * 条件更新批次处理元数据，过期或不存在时报告并发冲突。
     *
     * @param batch 更新后的批次
     * @param expectedStatus 更新前的状态
     * @param expectedUpdatedAt 更新前的更新时间
     * @throws io.asteria.reconciliation.domain.exception.ReconciliationDomainException 条件不匹配
     */
    void update(ReconciliationDeliveryBatch batch, ReconciliationFileBatchStatus expectedStatus, Instant expectedUpdatedAt);

    /**
     * 原子更新状态和更新时间，用于调用方抢占处理权；不判断业务状态转换规则。
     *
     * @param id 批次标识
     * @param originalStatus 原状态
     * @param targetStatus 目标状态
     * @return 条件匹配且更新成功时返回 true，否则返回 false
     */
    boolean updateStatus(ReconciliationDeliveryBatchId id, ReconciliationFileBatchStatus originalStatus,
                         ReconciliationFileBatchStatus targetStatus);

    /**
     * 按主键读取。
     */
    Optional<ReconciliationDeliveryBatch> findById(ReconciliationDeliveryBatchId id);
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
