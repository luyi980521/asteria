package io.asteria.reconciliation.infrastructure.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 运行中单一规则与业务键的不可变核对结论；重跑使用新的 Run，PENDING 不创建 Case。仅映射数据库行，不承担领域状态转换。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("reconciliation_match_result")
public class ReconciliationMatchResultDO {
    /** 主键，由 DistributedIdGenerator 生成正数雪花 ID。 */
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    /** 所属运行 ID，逻辑关联 reconciliation_run.id，无物理外键。 */
    private Long runId;

    /** 稳定规则编码，逻辑关联 reconciliation_rule.rule_code。 */
    private String ruleCode;

    /** 规则约定的跨来源业务关联键；须包含必要的租户或渠道命名空间。 */
    private String businessKey;

    /** 匹配状态，决定是否允许存在差异类型。 */
    private String status;

    /** 差异类型；MATCHED、PENDING 时为空，其余状态必填。 */
    private String differenceType;

    /** 基准侧金额，来源缺失时允许为空。 */
    private BigDecimal expectedAmount;

    /** 基准侧金额，来源缺失时允许为空所对应的币种；金额为空时币种也必须为空。 */
    private String expectedCurrency;

    /** 比较侧金额，可具有不同币种，来源缺失时允许为空。 */
    private BigDecimal actualAmount;

    /** 比较侧金额，可具有不同币种，来源缺失时允许为空所对应的币种；金额为空时币种也必须为空。 */
    private String actualCurrency;

    /** JsonUtils 序列化的非空正数快照主键数组；逻辑关联 source_record.id，禁止重复 ID。 */
    private String sourceRecordIds;

    /** 使用 JsonUtils 序列化的 JSON 说明对象，以 TEXT 原样保存。 */
    private String details;

    /** 记录创建时间，UTC 时间点。 */
    private Instant createdAt;
}
