package io.asteria.reconciliation.domain.enums;

/**
 * 差异工单追加式审计动作；不触发资金操作。
 */
public enum ReconciliationCaseActionType {
    /** 建立工单，无前置状态。 */
    OPEN,
    /** 从 OPEN 开始调查。 */
    INVESTIGATE,
    /** 关闭活动工单并记录解决结论。 */
    RESOLVE,
    /** 关闭活动工单并记录忽略原因。 */
    IGNORE,
    /** 追加说明，不改变状态。 */
    COMMENT
}
