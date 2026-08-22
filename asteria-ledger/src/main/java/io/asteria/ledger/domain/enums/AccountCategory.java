package io.asteria.ledger.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 账本账户会计分类
 * */
@Getter
@AllArgsConstructor
public enum AccountCategory {

    /** 资产 */
    ASSET(NormalBalance.DEBIT),

    /** 费用 */
    EXPENSE(NormalBalance.DEBIT),

    /** 负债 */
    LIABILITY(NormalBalance.CREDIT),

    /** 所有者权益 */
    EQUITY(NormalBalance.CREDIT),

    /** 收入 */
    REVENUE(NormalBalance.CREDIT),

    ;

    /** 正常余额方向 */
    private final NormalBalance normalBalance;
}
