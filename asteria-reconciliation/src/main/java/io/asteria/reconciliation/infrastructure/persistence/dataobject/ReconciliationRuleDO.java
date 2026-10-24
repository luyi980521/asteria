package io.asteria.reconciliation.infrastructure.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 对账规则定义；编码稳定，配置变更递增版本，Run 保存版本与快照以便审计。仅映射数据库行，不承担领域状态转换。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("reconciliation_rule")
public class ReconciliationRuleDO {
    /** 主键，由 DistributedIdGenerator 生成正数雪花 ID。 */
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    /** 稳定规则编码，逻辑关联 reconciliation_rule.rule_code。 */
    private String ruleCode;

    /** 业务可读的规则名称。 */
    private String name;

    /** 对账关系类型。 */
    private String type;

    /** 基准来源；v1 为 PAYMENT 或 SETTLEMENT。 */
    private String leftSourceType;

    /** 被核对来源；v1 必须为 LEDGER。 */
    private String rightSourceType;

    /** 是否允许创建新的对账运行。 */
    private boolean enabled;

    /** 等待异步入账的宽限秒数，非负；窗口结束加宽限后才可判定缺失。 */
    private Long gracePeriodSeconds;

    /** 使用 JsonUtils 序列化的 JSON 说明对象，以 TEXT 原样保存。 */
    private String details;

    /** 乐观锁版本；更新使用旧版本匹配并由数据库递增，更新后应重新读取。 */
    private Long version;

    /** 记录创建时间，UTC 时间点。 */
    private Instant createdAt;

    /** 最后更新时间，UTC 时间点且不得早于创建时间。 */
    private Instant updatedAt;
}
