package io.asteria.ledger.domain.enums;

public enum JournalSource {

    /** 支付 */
    PAYMENT,

    /** 退款 */
    REFUND,

    /** 转账 */
    TRANSFER,

    /** 提现 */
    WITHDRAW,

    /** 充值 */
    DEPOSIT
}