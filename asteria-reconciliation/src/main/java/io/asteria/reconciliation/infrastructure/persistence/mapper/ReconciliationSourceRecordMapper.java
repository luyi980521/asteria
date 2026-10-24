package io.asteria.reconciliation.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.asteria.reconciliation.infrastructure.persistence.dataobject.ReconciliationSourceRecordDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * reconciliation_source_record 的 MyBatis-Plus 映射；自定义 SQL 位于同名 XML。
 */
@Mapper
public interface ReconciliationSourceRecordMapper extends BaseMapper<ReconciliationSourceRecordDO> {
    /**
     * 按主键读取单行，避免加载关联记录集合。
     */
    ReconciliationSourceRecordDO findById(@Param("id") Long id);

    /**
     * 按来源幂等键读取不可变快照。
     */
    ReconciliationSourceRecordDO findBySource(@Param("sourceType") String sourceType, @Param("sourceRecordId") String sourceRecordId, @Param("sourceVersion") Long sourceVersion);
}
