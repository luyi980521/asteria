package io.asteria.reconciliation.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.asteria.reconciliation.infrastructure.persistence.dataobject.ReconciliationCaseDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * reconciliation_case 的 MyBatis-Plus 映射；自定义 SQL 位于同名 XML。
 */
@Mapper
public interface ReconciliationCaseMapper extends BaseMapper<ReconciliationCaseDO> {
    /**
     * 按主键读取单行，避免加载关联记录集合。
     */
    ReconciliationCaseDO findById(@Param("id") Long id);

    /**
     * 读取同类活动工单，最终并发去重仍由部分唯一索引保证。
     */
    ReconciliationCaseDO findActive(@Param("ruleCode") String ruleCode, @Param("businessKey") String businessKey, @Param("differenceType") String differenceType);

    /**
     * 仅修改可变字段，WHERE 校验旧版本，返回实际更新行数。
     */
    int updateWithVersion(ReconciliationCaseDO dataObject);
}
