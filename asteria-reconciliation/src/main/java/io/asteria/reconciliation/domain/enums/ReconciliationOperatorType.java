package io.asteria.reconciliation.domain.enums;

/**
 * 工单操作主体类型；系统操作同样必须记录可追溯的主体标识。
 */
public enum ReconciliationOperatorType {
    /** 系统或后台作业。 */
    SYSTEM,
    /** 人工操作主体。 */
    HUMAN
}
