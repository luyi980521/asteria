package io.asteria.reconciliation.domain.entity;

import io.asteria.reconciliation.domain.enums.ReconciliationCaseActionType;
import io.asteria.reconciliation.domain.enums.ReconciliationCaseStatus;
import io.asteria.reconciliation.domain.enums.ReconciliationOperatorType;
import io.asteria.reconciliation.domain.valueobject.ReconciliationCaseActionId;
import io.asteria.reconciliation.domain.valueobject.ReconciliationCaseId;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

import static io.asteria.reconciliation.domain.entity.ReconciliationValidation.*;

/**
 * 工单追加式审计记录；仅记录操作，不执行资金修复，必须与工单变更在同一事务保存。
 */
@Getter
public final class ReconciliationCaseAction {
    /** 主键，由 DistributedIdGenerator 生成正数雪花 ID。 */
    private final ReconciliationCaseActionId caseActionId;

    /** 工单 ID，逻辑关联 reconciliation_case.id，无物理外键。 */
    private final ReconciliationCaseId caseId;

    /** 审计动作，限定合法的前后状态组合。 */
    private final ReconciliationCaseActionType actionType;

    /** 操作主体类型 SYSTEM 或 HUMAN。 */
    private final ReconciliationOperatorType operatorType;

    /** 可追溯的系统或人工主体标识，系统操作也必填。 */
    private final String operatorId;

    /** 操作前状态，仅 OPEN 动作允许为空。 */
    private final ReconciliationCaseStatus fromStatus;

    /** 操作后状态；COMMENT 时必须与前状态相同。 */
    private final ReconciliationCaseStatus toStatus;

    /** 操作说明，解决和忽略动作必须给出原因。 */
    private final String reason;

    /** 使用 JsonUtils 序列化的 JSON 说明对象，以 TEXT 原样保存。 */
    private final String details;

    /** 记录创建时间，UTC 时间点。 */
    private final Instant createdAt;

    /**
     * 构造或恢复领域对象；Builder 同样执行校验，不能绕过数据及状态约束。
     */
    @Builder
    private ReconciliationCaseAction(
            ReconciliationCaseActionId caseActionId,
            ReconciliationCaseId caseId,
            ReconciliationCaseActionType actionType,
            ReconciliationOperatorType operatorType,
            String operatorId,
            ReconciliationCaseStatus fromStatus,
            ReconciliationCaseStatus toStatus,
            String reason,
            String details,
            Instant createdAt) {
        require(caseActionId != null);
        require(caseId != null);
        require(actionType != null);
        require(operatorType != null);
        text(operatorId, 128);
        require(toStatus != null);
        json(details, false);
        require(createdAt != null);
        text(reason, Integer.MAX_VALUE);
        require(switch (actionType) {
            case OPEN -> fromStatus == null && toStatus == ReconciliationCaseStatus.OPEN;
            case INVESTIGATE -> fromStatus == ReconciliationCaseStatus.OPEN && toStatus == ReconciliationCaseStatus.INVESTIGATING;
            case RESOLVE -> active(fromStatus) && toStatus == ReconciliationCaseStatus.RESOLVED;
            case IGNORE -> active(fromStatus) && toStatus == ReconciliationCaseStatus.IGNORED;
            case COMMENT -> fromStatus != null && fromStatus == toStatus;
        });
        this.caseActionId = caseActionId;
        this.caseId = caseId;
        this.actionType = actionType;
        this.operatorType = operatorType;
        this.operatorId = operatorId;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.reason = reason;
        this.details = details;
        this.createdAt = createdAt;
    }

    /**
     * 活动状态才能产生解决或忽略动作，审计记录不能描述不存在的状态转换。
     */
    private static boolean active(ReconciliationCaseStatus status) {
        return status == ReconciliationCaseStatus.OPEN || status == ReconciliationCaseStatus.INVESTIGATING;
    }
}
