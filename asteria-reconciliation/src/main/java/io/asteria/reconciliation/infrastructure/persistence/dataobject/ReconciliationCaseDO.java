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
 * 跨运行跟踪的差异工单；OPEN、INVESTIGATING 属于活动状态，解决或忽略后允许后续新建。仅映射数据库行，不承担领域状态转换。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("reconciliation_case")
public class ReconciliationCaseDO {
    /** 主键，由 DistributedIdGenerator 生成正数雪花 ID。 */
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    /** 稳定规则编码，逻辑关联 reconciliation_rule.rule_code。 */
    private String ruleCode;

    /** 规则约定的跨来源业务关联键；须包含必要的租户或渠道命名空间。 */
    private String businessKey;

    /** 工单差异类型，参与活动工单唯一性。 */
    private String differenceType;

    /** 首次发现差异的运行 ID，逻辑关联 reconciliation_run.id。 */
    private Long runId;

    /** 首次差异结论 ID，逻辑关联 reconciliation_match_result.id。 */
    private Long matchResultId;

    /** OPEN、INVESTIGATING、RESOLVED 或 IGNORED。 */
    private String status;

    /** 调查责任人标识，INVESTIGATING 必填。 */
    private String assignee;

    /** 使用 JsonUtils 序列化的 JSON 说明对象，以 TEXT 原样保存。 */
    private String details;

    /** 解决结论或忽略原因，终态必填。 */
    private String resolution;

    /** 解决或忽略的时间，活动状态必须为空。 */
    private Instant closedAt;

    /** 乐观锁版本；更新使用旧版本匹配并由数据库递增，更新后应重新读取。 */
    private Long version;

    /** 记录创建时间，UTC 时间点。 */
    private Instant createdAt;

    /** 最后更新时间，UTC 时间点且不得早于创建时间。 */
    private Instant updatedAt;
}
