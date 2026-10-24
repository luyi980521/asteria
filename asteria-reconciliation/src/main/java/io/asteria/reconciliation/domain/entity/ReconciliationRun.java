package io.asteria.reconciliation.domain.entity;

import io.asteria.common.util.JsonUtils;
import io.asteria.reconciliation.domain.enums.ReconciliationRunStatus;
import io.asteria.reconciliation.domain.enums.ReconciliationType;
import io.asteria.reconciliation.domain.error.ReconciliationErrorCode;
import io.asteria.reconciliation.domain.exception.ReconciliationDomainException;
import io.asteria.reconciliation.domain.valueobject.ReconciliationRunId;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

import static io.asteria.reconciliation.domain.entity.ReconciliationValidation.*;

/**
 * 一次规则执行的状态和计数；窗口为 [windowStart, windowEnd)，不持有来源及结果集合。
 */
@Getter
public final class ReconciliationRun {
    /** 主键，由 DistributedIdGenerator 生成正数雪花 ID。 */
    private final ReconciliationRunId runId;

    /** 稳定规则编码，逻辑关联 reconciliation_rule.rule_code。 */
    private final String ruleCode;

    /** 运行选用的规则乐观锁版本，用于追溯规则配置。 */
    private final Long ruleVersion;

    /** 运行选用的对账类型快照。 */
    private final ReconciliationType type;

    /** 使用 JsonUtils 序列化的运行时完整规则 JSON 快照。 */
    private final String ruleSnapshot;

    /** 对账业务窗口起点，包含此时刻。 */
    private final Instant windowStart;

    /** 对账业务窗口终点，不包含此时刻。 */
    private final Instant windowEnd;

    /** 采集可见性截止时间，只使用 created_at 小于等于此值的来源快照。 */
    private final Instant sourceCutoffAt;

    /** CREATED、RUNNING、COMPLETED 或 FAILED；终态不可重启。 */
    private ReconciliationRunStatus status;

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
    private final Long version;

    /** 记录创建时间，UTC 时间点。 */
    private final Instant createdAt;

    /** 最后更新时间，UTC 时间点且不得早于创建时间。 */
    private Instant updatedAt;

    /**
     * 构造或恢复领域对象；Builder 同样执行校验，不能绕过数据及状态约束。
     */
    @Builder
    private ReconciliationRun(
            ReconciliationRunId runId,
            String ruleCode,
            Long ruleVersion,
            ReconciliationType type,
            String ruleSnapshot,
            Instant windowStart,
            Instant windowEnd,
            Instant sourceCutoffAt,
            ReconciliationRunStatus status,
            Long totalCount,
            Long matchedCount,
            Long mismatchedCount,
            Long missingCount,
            Long pendingCount,
            Long duplicatedCount,
            Instant startedAt,
            Instant finishedAt,
            String failureReason,
            Long version,
            Instant createdAt,
            Instant updatedAt) {
        require(runId != null);
        text(ruleCode, 64);
        require(ruleVersion != null);
        require(type != null);
        require(windowStart != null);
        require(windowEnd != null);
        require(sourceCutoffAt != null);
        require(status != null);
        require(totalCount != null);
        require(matchedCount != null);
        require(mismatchedCount != null);
        require(missingCount != null);
        require(pendingCount != null);
        require(duplicatedCount != null);
        require(version != null);
        require(createdAt != null);
        require(updatedAt != null);
        require(version >= 0);
        times(createdAt, updatedAt);
        require(ruleVersion >= 0 && windowStart.isBefore(windowEnd)
                && !sourceCutoffAt.isBefore(windowEnd) && !sourceCutoffAt.isAfter(createdAt));
        json(ruleSnapshot, true);
        validateCounts(totalCount, matchedCount, mismatchedCount, missingCount, pendingCount, duplicatedCount);
        require((status == ReconciliationRunStatus.CREATED && startedAt == null && finishedAt == null)
                || (status == ReconciliationRunStatus.RUNNING && startedAt != null && finishedAt == null)
                || ((status == ReconciliationRunStatus.COMPLETED || status == ReconciliationRunStatus.FAILED)
                    && startedAt != null && finishedAt != null));
        require(startedAt == null || (!startedAt.isBefore(createdAt) && !startedAt.isAfter(updatedAt)));
        require(finishedAt == null || (!finishedAt.isBefore(startedAt) && !finishedAt.isAfter(updatedAt)));
        require(status == ReconciliationRunStatus.FAILED
                ? failureReason != null && !failureReason.isBlank() : failureReason == null);
        require(status == ReconciliationRunStatus.COMPLETED || totalCount == 0);
        this.runId = runId;
        this.ruleCode = ruleCode;
        this.ruleVersion = ruleVersion;
        this.type = type;
        this.ruleSnapshot = ruleSnapshot;
        this.windowStart = windowStart;
        this.windowEnd = windowEnd;
        this.sourceCutoffAt = sourceCutoffAt;
        this.status = status;
        this.totalCount = totalCount;
        this.matchedCount = matchedCount;
        this.mismatchedCount = mismatchedCount;
        this.missingCount = missingCount;
        this.pendingCount = pendingCount;
        this.duplicatedCount = duplicatedCount;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
        this.failureReason = failureReason;
        this.version = version;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * 冻结规则及可见性截止时间；窗口和规则快照在整个运行期间不可变。
     */
    public static ReconciliationRun create(ReconciliationRunId runId, ReconciliationRule rule,
                                           Instant windowStart, Instant windowEnd, Instant sourceCutoffAt, Instant now) {
        require(rule != null);
        state(rule.isEnabled());
        return builder().runId(runId).ruleCode(rule.getRuleCode()).ruleVersion(rule.getVersion()).type(rule.getType())
                .ruleSnapshot(JsonUtils.toJson(rule))
                .windowStart(windowStart).windowEnd(windowEnd).sourceCutoffAt(sourceCutoffAt)
                .status(ReconciliationRunStatus.CREATED).totalCount(0L).matchedCount(0L).mismatchedCount(0L)
                .missingCount(0L).pendingCount(0L).duplicatedCount(0L).version(0L).createdAt(now).updatedAt(now).build();
    }

    /**
     * 仅已创建的运行可开始；终态重跑必须申请新的 Run ID。
     */
    public void start(Instant now) {
        state(status == ReconciliationRunStatus.CREATED);
        nextTime(updatedAt, now);
        status = ReconciliationRunStatus.RUNNING;
        startedAt = now;
        updatedAt = now;
    }

    /**
     * 完成运行；分类计数必须恰好覆盖全部业务键，差异不等于运行失败。
     */
    public void complete(Long total, Long matched, Long mismatched, Long missing, Long pending, Long duplicated, Instant now) {
        state(status == ReconciliationRunStatus.RUNNING);
        validateCounts(total, matched, mismatched, missing, pending, duplicated);
        nextTime(updatedAt, now);
        totalCount = total;
        matchedCount = matched;
        mismatchedCount = mismatched;
        missingCount = missing;
        pendingCount = pending;
        duplicatedCount = duplicated;
        status = ReconciliationRunStatus.COMPLETED;
        finishedAt = now;
        updatedAt = now;
    }

    /**
     * 仅运行中的任务可失败，原因必填；v1 不自动恢复或重启。
     */
    public void fail(String reason, Instant now) {
        state(status == ReconciliationRunStatus.RUNNING);
        text(reason, Integer.MAX_VALUE);
        nextTime(updatedAt, now);
        status = ReconciliationRunStatus.FAILED;
        failureReason = reason;
        finishedAt = now;
        updatedAt = now;
    }

    /**
     * 判定业务时间是否属于左闭右开的窗口。
     */
    public boolean contains(Instant occurredAt) {
        require(occurredAt != null);
        return !occurredAt.isBefore(windowStart) && occurredAt.isBefore(windowEnd);
    }

    /**
     * 使用精确加法拒绝溢出，确保总计数与互斥分类计数一致。
     */
    private static void validateCounts(Long total, Long matched, Long mismatched, Long missing, Long pending, Long duplicated) {
        require(total != null && matched != null && mismatched != null && missing != null && pending != null
                && duplicated != null && total >= 0 && matched >= 0 && mismatched >= 0
                && missing >= 0 && pending >= 0 && duplicated >= 0);
        try {
            long sum = Math.addExact(Math.addExact(Math.addExact(matched, mismatched), Math.addExact(missing, pending)), duplicated);
            require(total == sum);
        } catch (ArithmeticException exception) {
            throw new ReconciliationDomainException(ReconciliationErrorCode.INVALID_PARAMS, exception);
        }
    }
}
