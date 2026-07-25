package io.asteria.ledger.domain.enums;

public enum DebitCredit {

    /** 借 */
    DEBIT,

    /** 贷 */
    CREDIT,

    ;

    public DebitCredit reverse() {
        return this == DEBIT ? CREDIT : DEBIT;
    }
}
