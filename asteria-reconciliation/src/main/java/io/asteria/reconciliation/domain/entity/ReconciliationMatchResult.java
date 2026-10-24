package io.asteria.reconciliation.domain.entity;

import io.asteria.common.domain.valueobject.Money;
import io.asteria.reconciliation.domain.enums.ReconciliationDifferenceType;
import io.asteria.reconciliation.domain.enums.ReconciliationMatchStatus;
import io.asteria.reconciliation.domain.valueobject.ReconciliationMatchResultId;
import io.asteria.reconciliation.domain.valueobject.ReconciliationRunId;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

import static io.asteria.reconciliation.domain.entity.ReconciliationValidation.*;

/**
 * 运行中单一规则与业务键的不可变核对结论；重跑使用新的 Run，PENDING 不创建 Case。
 */
@Getter
public final class ReconciliationMatchResult {
    /** 主键，由 DistributedIdGenerator 生成正数雪花 ID。 */
    private final ReconciliationMatchResultId matchResultId;

    /** 所属运行 ID，逻辑关联 reconciliation_run.id，无物理外键。 */
    private final ReconciliationRunId runId;

    /** 稳定规则编码，逻辑关联 reconciliation_rule.rule_code。 */
    private final String ruleCode;

    /** 规则约定的跨来源业务关联键；须包含必要的租户或渠道命名空间。 */
    private final String businessKey;

    /** 匹配状态，决定是否允许存在差异类型。 */
    private final ReconciliationMatchStatus status;

    /** 差异类型；MATCHED、PENDING 时为空，其余状态必填。 */
    private final ReconciliationDifferenceType differenceType;

    /** 基准侧金额，来源缺失时允许为空。 */
    private final Money expectedAmount;

    /** 比较侧金额，可具有不同币种，来源缺失时允许为空。 */
    private final Money actualAmount;

    /** JsonUtils 序列化的非空正数快照主键数组；逻辑关联 source_record.id，禁止重复 ID。 */
    private final String sourceRecordIds;

    /** 使用 JsonUtils 序列化的 JSON 说明对象，以 TEXT 原样保存。 */
    private final String details;

    /** 记录创建时间，UTC 时间点。 */
    private final Instant createdAt;

    /**
     * 构造或恢复领域对象；Builder 同样执行校验，不能绕过数据及状态约束。
     */
    @Builder
    private ReconciliationMatchResult(
            ReconciliationMatchResultId matchResultId,
            ReconciliationRunId runId,
            String ruleCode,
            String businessKey,
            ReconciliationMatchStatus status,
            ReconciliationDifferenceType differenceType,
            Money expectedAmount,
            Money actualAmount,
            String sourceRecordIds,
            String details,
            Instant createdAt) {
        require(matchResultId != null);
        require(runId != null);
        text(ruleCode, 64);
        text(businessKey, 256);
        require(status != null);
        money(expectedAmount);
        money(actualAmount);
        json(details, false);
        require(createdAt != null);
        sourceIds(sourceRecordIds);
        require(switch (status) {
            case MATCHED, PENDING -> differenceType == null;
            case MISSING -> differenceType == ReconciliationDifferenceType.MISSING_PAYMENT
                    || differenceType == ReconciliationDifferenceType.MISSING_LEDGER
                    || differenceType == ReconciliationDifferenceType.MISSING_SETTLEMENT;
            case DUPLICATED -> differenceType == ReconciliationDifferenceType.DUPLICATE_RECORD;
            case MISMATCHED -> differenceType == ReconciliationDifferenceType.AMOUNT_MISMATCH
                    || differenceType == ReconciliationDifferenceType.CURRENCY_MISMATCH
                    || differenceType == ReconciliationDifferenceType.STATUS_MISMATCH
                    || differenceType == ReconciliationDifferenceType.POSTING_MISMATCH
                    || differenceType == ReconciliationDifferenceType.UNEXPECTED_RECORD;
        });
        this.matchResultId = matchResultId;
        this.runId = runId;
        this.ruleCode = ruleCode;
        this.businessKey = businessKey;
        this.status = status;
        this.differenceType = differenceType;
        this.expectedAmount = expectedAmount;
        this.actualAmount = actualAmount;
        this.sourceRecordIds = sourceRecordIds;
        this.details = details;
        this.createdAt = createdAt;
    }
}
