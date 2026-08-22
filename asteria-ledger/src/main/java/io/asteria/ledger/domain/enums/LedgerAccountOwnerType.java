package io.asteria.ledger.domain.enums;

/**
 * 账本账户所属主体类型
 * */
public enum LedgerAccountOwnerType {

    /** 用户 */
    USER,

    /** 商户 */
    MERCHANT,

    /** 平台 */
    PLATFORM,

    /** 支付渠道 */
    CHANNEL,

    /** 银行 */
    BANK,

    /** 系统内部主体 */
    SYSTEM
}
