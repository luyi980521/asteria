package io.asteria.reconciliation.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.asteria.reconciliation.infrastructure.persistence.dataobject.ReconciliationRunDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * reconciliation_run 的 MyBatis-Plus 映射；自定义 SQL 位于同名 XML。
 */
@Mapper
public interface ReconciliationRunMapper extends BaseMapper<ReconciliationRunDO> {
    /**
     * 按主键读取单行，避免加载关联记录集合。
     */
    ReconciliationRunDO findById(@Param("id") Long id);

    /**
     * 仅修改可变字段，WHERE 校验旧版本，返回实际更新行数。
     */
    int updateWithVersion(ReconciliationRunDO dataObject);
}
