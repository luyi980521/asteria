package io.asteria.reconciliation.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.asteria.reconciliation.infrastructure.persistence.dataobject.ReconciliationRuleDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * reconciliation_rule 的 MyBatis-Plus 映射；自定义 SQL 位于同名 XML。
 */
@Mapper
public interface ReconciliationRuleMapper extends BaseMapper<ReconciliationRuleDO> {
    /**
     * 按主键读取单行，避免加载关联记录集合。
     */
    ReconciliationRuleDO findById(@Param("id") Long id);

    /**
     * 按稳定规则编码读取当前版本。
     */
    ReconciliationRuleDO findByRuleCode(@Param("ruleCode") String ruleCode);

    /**
     * 仅修改可变字段，WHERE 校验旧版本，返回实际更新行数。
     */
    int updateWithVersion(ReconciliationRuleDO dataObject);
}
