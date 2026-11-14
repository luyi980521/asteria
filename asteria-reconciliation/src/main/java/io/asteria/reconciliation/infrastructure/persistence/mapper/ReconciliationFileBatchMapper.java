package io.asteria.reconciliation.infrastructure.persistence.mapper;

import io.asteria.reconciliation.infrastructure.persistence.dataobject.ReconciliationFileBatchDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * reconciliation_file_batch 映射，SQL 位于同名 XML。
 */
@Mapper
public interface ReconciliationFileBatchMapper extends BaseMapper<ReconciliationFileBatchDO> {
    /**
     * 读取主键对应的元数据。
     */
    ReconciliationFileBatchDO findById(@Param("id") Long id);
    /**
     * 读取交付唯一键对应的元数据。
     */
    ReconciliationFileBatchDO findByDelivery(@Param("sourceSystem") String sourceSystem, @Param("objectKey") String objectKey, @Param("fileVersion") Long fileVersion);
    /**
     * 按来源与业务日期分页。
     */
    List<ReconciliationFileBatchDO> findBySourceAndBusinessDate(@Param("sourceSystem") String sourceSystem, @Param("businessDate") LocalDate businessDate, @Param("afterId") long afterId, @Param("limit") int limit);
    /**
     * 按状态分页。
     */
    List<ReconciliationFileBatchDO> findByStatus(@Param("status") String status, @Param("afterId") long afterId, @Param("limit") int limit);
    /**
     * 原子登记，不覆盖已有交付。
     */
    int insertIfAbsent(ReconciliationFileBatchDO row);
    /**
     * 条件更新基础处理元数据。
     */
    int updateMetadata(@Param("row") ReconciliationFileBatchDO row, @Param("expectedStatus") String expectedStatus, @Param("expectedUpdatedAt") Instant expectedUpdatedAt);
    /**
     * 按交付批次读取有界文件页。
     */
    List<ReconciliationFileBatchDO> findByBatchId(@Param("batchId") Long batchId, @Param("afterId") long afterId, @Param("limit") int limit);
    /**
     * 兼容旧调用方的状态条件更新，不判断转换策略。
     */
    int updateImportStatus(@Param("fileId") Long fileId, @Param("expectedStatus") String expectedStatus,
            @Param("status") String status, @Param("completedAt") Instant completedAt, @Param("failureReason") String failureReason);
}