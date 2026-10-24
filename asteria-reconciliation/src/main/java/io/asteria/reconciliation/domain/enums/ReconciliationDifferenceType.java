package io.asteria.reconciliation.domain.enums;

/**
 * 差异分类；活动 Case 按规则、业务键与此分类去重。
 */
public enum ReconciliationDifferenceType {
    /** 缺少支付记录。 */
    MISSING_PAYMENT,
    /** 缺少账本记录。 */
    MISSING_LEDGER,
    /** 缺少结算记录。 */
    MISSING_SETTLEMENT,
    /** 金额不一致。 */
    AMOUNT_MISMATCH,
    /** 币种不一致。 */
    CURRENCY_MISMATCH,
    /** 业务状态不一致。 */
    STATUS_MISMATCH,
    /** 记账分录或借贷方向不一致。 */
    POSTING_MISMATCH,
    /** 同一业务存在重复记录。 */
    DUPLICATE_RECORD,
    /** 存在规则不预期的记录。 */
    UNEXPECTED_RECORD
}
