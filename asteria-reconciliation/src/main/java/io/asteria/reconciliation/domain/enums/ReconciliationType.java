package io.asteria.reconciliation.domain.enums;

/**
 * 对账关系类型；v1 仅定义支付与结算分别核对账本。
 */
public enum ReconciliationType {
    /** 支付业务与账本核对。 */
    PAYMENT_LEDGER,
    /** 结算业务与账本核对。 */
    SETTLEMENT_LEDGER
}
