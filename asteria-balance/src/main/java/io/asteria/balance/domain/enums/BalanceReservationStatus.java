package io.asteria.balance.domain.enums;

public enum BalanceReservationStatus {

    /** 预留生效中，资金已从可用余额转入预留余额，等待后续处理 */
    RESERVED,

    /** 预留已解除，资金退回可用余额，例如业务取消 */
    RELEASED,

    /** 预留资金已被正式扣除，例如业务完成 */
    CONSUMED
}
