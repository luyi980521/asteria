package io.asteria.reconciliation.domain.enums;

/**
 * 差异工单状态；OPEN、INVESTIGATING 为活动状态，终态不允许重新打开。
 */
public enum ReconciliationCaseStatus {
    /** 已发现差异，等待调查。 */
    OPEN,
    /** 已分配处理人并开始调查。 */
    INVESTIGATING,
    /** 已解决，必须记录处理结论。 */
    RESOLVED,
    /** 已忽略，必须说明忽略原因。 */
    IGNORED
}
