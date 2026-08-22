package io.asteria.ledger.domain.enums;

/**
 * 账户正常余额方向
 * */
public enum NormalBalance {

    /** 借方余额 */
    DEBIT,

    /** 贷方余额 */
    CREDIT;

    /**
     * 转换为借贷方向
     * */
    public DebitCredit toDebitCredit() {
        return this == DEBIT ? DebitCredit.DEBIT : DebitCredit.CREDIT;
    }
}