package io.asteria.reconciliation.domain.enums;

/**
 * 对账运行状态；终态不可重新运行，重跑应创建新的 Run。
 */
public enum ReconciliationRunStatus {
    /** 已创建，尚未读取来源。 */
    CREATED,
    /** 运行中，可完成或失败。 */
    RUNNING,
    /** 已完成，仍可存在差异或待确认结果。 */
    COMPLETED,
    /** 运行失败，保留失败原因。 */
    FAILED
}
