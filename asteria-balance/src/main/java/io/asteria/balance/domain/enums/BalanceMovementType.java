package io.asteria.balance.domain.enums;

public enum BalanceMovementType {

    /** 可用金额增加，预留金额不变 */
    CREDIT,

    /** 可用金额减少，预留金额增加 */
    RESERVE,

    /** 可用金额增加，预留金额减少 */
    RELEASE,

    /** 可用金额不变，预留金额减少 */
    DEBIT_RESERVED
}
