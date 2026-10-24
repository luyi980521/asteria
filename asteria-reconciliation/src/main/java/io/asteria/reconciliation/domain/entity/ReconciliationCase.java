package io.asteria.reconciliation.domain.entity;

import io.asteria.reconciliation.domain.enums.ReconciliationCaseStatus;
import io.asteria.reconciliation.domain.enums.ReconciliationDifferenceType;
import io.asteria.reconciliation.domain.valueobject.ReconciliationCaseId;
import io.asteria.reconciliation.domain.valueobject.ReconciliationMatchResultId;
import io.asteria.reconciliation.domain.valueobject.ReconciliationRunId;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

import static io.asteria.reconciliation.domain.entity.ReconciliationValidation.*;

/**
 * 跨运行跟踪的差异工单；OPEN、INVESTIGATING 属于活动状态，解决或忽略后允许后续新建。
 */
@Getter
public final class ReconciliationCase {
    /** 主键，由 DistributedIdGenerator 生成正数雪花 ID。 */
    private final ReconciliationCaseId caseId;

    /** 稳定规则编码，逻辑关联 reconciliation_rule.rule_code。 */
    private final String ruleCode;

    /** 规则约定的跨来源业务关联键；须包含必要的租户或渠道命名空间。 */
    private final String businessKey;

    /** 工单差异类型，参与活动工单唯一性。 */
    private final ReconciliationDifferenceType differenceType;

    /** 首次发现差异的运行 ID，逻辑关联 reconciliation_run.id。 */
    private final ReconciliationRunId runId;

    /** 首次差异结论 ID，逻辑关联 reconciliation_match_result.id。 */
    private final ReconciliationMatchResultId matchResultId;

    /** OPEN、INVESTIGATING、RESOLVED 或 IGNORED。 */
    private ReconciliationCaseStatus status;

    /** 调查责任人标识，INVESTIGATING 必填。 */
    private String assignee;

    /** 使用 JsonUtils 序列化的 JSON 说明对象，以 TEXT 原样保存。 */
    private final String details;

    /** 解决结论或忽略原因，终态必填。 */
    private String resolution;

    /** 解决或忽略的时间，活动状态必须为空。 */
    private Instant closedAt;

    /** 乐观锁版本；更新使用旧版本匹配并由数据库递增，更新后应重新读取。 */
    private final Long version;

    /** 记录创建时间，UTC 时间点。 */
    private final Instant createdAt;

    /** 最后更新时间，UTC 时间点且不得早于创建时间。 */
    private Instant updatedAt;

    /**
     * 构造或恢复领域对象；Builder 同样执行校验，不能绕过数据及状态约束。
     */
    @Builder
    private ReconciliationCase(
            ReconciliationCaseId caseId,
            String ruleCode,
            String businessKey,
            ReconciliationDifferenceType differenceType,
            ReconciliationRunId runId,
            ReconciliationMatchResultId matchResultId,
            ReconciliationCaseStatus status,
            String assignee,
            String details,
            String resolution,
            Instant closedAt,
            Long version,
            Instant createdAt,
            Instant updatedAt) {
        require(caseId != null);
        text(ruleCode, 64);
        text(businessKey, 256);
        require(differenceType != null);
        require(runId != null);
        require(matchResultId != null);
        require(status != null);
        if (assignee != null) { text(assignee, 128); }
        json(details, false);
        require(version != null);
        require(createdAt != null);
        require(updatedAt != null);
        require(version >= 0);
        times(createdAt, updatedAt);
        require(status != ReconciliationCaseStatus.INVESTIGATING || assignee != null);
        boolean terminal = status == ReconciliationCaseStatus.RESOLVED || status == ReconciliationCaseStatus.IGNORED;
        require(terminal ? closedAt != null && resolution != null && !resolution.isBlank()
                : closedAt == null && resolution == null);
        require(closedAt == null || (!closedAt.isBefore(createdAt) && !closedAt.isAfter(updatedAt)));
        this.caseId = caseId;
        this.ruleCode = ruleCode;
        this.businessKey = businessKey;
        this.differenceType = differenceType;
        this.runId = runId;
        this.matchResultId = matchResultId;
        this.status = status;
        this.assignee = assignee;
        this.details = details;
        this.resolution = resolution;
        this.closedAt = closedAt;
        this.version = version;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * 从真实差异结论建立工单；MATCHED、PENDING 不允许建案。
     */
    public static ReconciliationCase open(ReconciliationCaseId caseId, ReconciliationMatchResult result, Instant now) {
        require(result != null);
        state(result.getDifferenceType() != null);
        nextTime(result.getCreatedAt(), now);
        return builder().caseId(caseId).ruleCode(result.getRuleCode()).businessKey(result.getBusinessKey())
                .differenceType(result.getDifferenceType()).runId(result.getRunId()).matchResultId(result.getMatchResultId())
                .status(ReconciliationCaseStatus.OPEN).details(result.getDetails()).version(0L)
                .createdAt(now).updatedAt(now).build();
    }

    /**
     * OPEN 工单进入调查，必须分配责任人。
     */
    public void investigate(String assignee, Instant now) {
        state(status == ReconciliationCaseStatus.OPEN);
        text(assignee, 128);
        nextTime(updatedAt, now);
        this.assignee = assignee;
        status = ReconciliationCaseStatus.INVESTIGATING;
        updatedAt = now;
    }

    /**
     * 解决活动工单；结论必填，不执行任何资金或记账操作。
     */
    public void resolve(String resolution, Instant now) {
        close(ReconciliationCaseStatus.RESOLVED, resolution, now);
    }

    /**
     * 忽略活动工单；原因必填，终态不可再次变更。
     */
    public void ignore(String reason, Instant now) {
        close(ReconciliationCaseStatus.IGNORED, reason, now);
    }

    /**
     * 只允许活动工单关闭，释放部分唯一索引中的活动键。
     */
    private void close(ReconciliationCaseStatus target, String reason, Instant now) {
        state(status == ReconciliationCaseStatus.OPEN || status == ReconciliationCaseStatus.INVESTIGATING);
        text(reason, Integer.MAX_VALUE);
        nextTime(updatedAt, now);
        status = target;
        resolution = reason;
        closedAt = now;
        updatedAt = now;
    }
}
