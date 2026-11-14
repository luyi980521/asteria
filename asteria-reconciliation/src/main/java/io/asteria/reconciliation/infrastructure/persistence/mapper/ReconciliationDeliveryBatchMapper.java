package io.asteria.reconciliation.infrastructure.persistence.mapper;

import io.asteria.reconciliation.infrastructure.persistence.dataobject.ReconciliationDeliveryBatchDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * reconciliation_delivery_batch 映射，SQL 位于同名 XML。
 */
@Mapper
public interface ReconciliationDeliveryBatchMapper extends BaseMapper<ReconciliationDeliveryBatchDO> {
    /**
     * 读取主键对应的元数据。
     */
    ReconciliationDeliveryBatchDO findById(@Param("id") Long id);
    /**
     * 读取交付唯一键对应的元数据。
     */
    ReconciliationDeliveryBatchDO findByDelivery(@Param("sourceSystem") String sourceSystem, @Param("objectKey") String objectKey, @Param("fileVersion") Long fileVersion);
    /**
     * 按来源与业务日期分页。
     */
    List<ReconciliationDeliveryBatchDO> findBySourceAndBusinessDate(@Param("sourceSystem") String sourceSystem, @Param("businessDate") LocalDate businessDate, @Param("afterId") long afterId, @Param("limit") int limit);
    /**
     * 按状态分页。
     */
    List<ReconciliationDeliveryBatchDO> findByStatus(@Param("status") String status, @Param("afterId") long afterId, @Param("limit") int limit);
    /**
     * 原子登记，不覆盖已有交付。
     */
    int insertIfAbsent(ReconciliationDeliveryBatchDO row);
    /**
     * 条件更新基础处理元数据。
     */
    int updateMetadata(@Param("row") ReconciliationDeliveryBatchDO row, @Param("expectedStatus") String expectedStatus, @Param("expectedUpdatedAt") Instant expectedUpdatedAt);

}