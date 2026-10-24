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
 * 一次规则执行的状态和计数；窗口为 [windowStart, windowEnd)，不持有来源及结果集合。仅映射数据库行，不承担领域状态转换。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("reconciliation_run")
public class ReconciliationRunDO {
    /** 主键，由 DistributedIdGenerator 生成正数雪花 ID。 */
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    /** 稳定规则编码，逻辑关联 reconciliation_rule.rule_code。 */
    private String ruleCode;

    /** 运行选用的规则乐观锁版本，用于追溯规则配置。 */
    private Long ruleVersion;

    /** 运行选用的对账类型快照。 */
    private String type;

    /** 使用 JsonUtils 序列化的运行时完整规则 JSON 快照。 */
    private String ruleSnapshot;

    /** 对账业务窗口起点，包含此时刻。 */
    private Instant windowStart;

    /** 对账业务窗口终点，不包含此时刻。 */
    private Instant windowEnd;

    /** 采集可见性截止时间，只使用 created_at 小于等于此值的来源快照。 */
    private Instant sourceCutoffAt;

    /** CREATED、RUNNING、COMPLETED 或 FAILED；终态不可重启。 */
    private String status;

    /** 本次已评估的业务键总数，不是来源记录数。 */
    private Long totalCount;

    /** matched 状态的业务键数量。 */
    private Long matchedCount;

    /** mismatched 状态的业务键数量。 */
    private Long mismatchedCount;

    /** missing 状态的业务键数量。 */
    private Long missingCount;

    /** pending 状态的业务键数量。 */
    private Long pendingCount;

    /** duplicated 状态的业务键数量。 */
    private Long duplicatedCount;

    /** 开始运行时间，仅 RUNNING 或终态存在。 */
    private Instant startedAt;

    /** 运行完成或失败时间，只有终态存在。 */
    private Instant finishedAt;

    /** 失败原因；FAILED 状态必填。 */
    private String failureReason;

    /** 乐观锁版本；更新使用旧版本匹配并由数据库递增，更新后应重新读取。 */
    private Long version;

    /** 记录创建时间，UTC 时间点。 */
    private Instant createdAt;

    /** 最后更新时间，UTC 时间点且不得早于创建时间。 */
    private Instant updatedAt;
}
