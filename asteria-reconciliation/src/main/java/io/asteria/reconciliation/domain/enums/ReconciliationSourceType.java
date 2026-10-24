package io.asteria.reconciliation.domain.enums;

/**
 * 来源系统类型；与来源业务记录 ID、版本共同确定快照唯一性。
 */
public enum ReconciliationSourceType {
    /** 支付系统。 */
    PAYMENT,
    /** 账本系统。 */
    LEDGER,
    /** 结算系统。 */
    SETTLEMENT,
    /** 余额系统。 */
    BALANCE,
    /** 支付或结算渠道。 */
    CHANNEL,
    /** 银行流水。 */
    BANK,
    /** 基金登记过户系统。 */
    FUND_TA
}
