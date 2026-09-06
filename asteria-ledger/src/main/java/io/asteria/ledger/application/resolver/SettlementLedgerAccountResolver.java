package io.asteria.ledger.application.resolver;

import io.asteria.common.domain.valueobject.CurrencyCode;
import io.asteria.ledger.application.model.SettlementCompletedLedgerAccounts;

/**
 * Settlement 记账账户解析器
 */
public interface SettlementLedgerAccountResolver {

    /**
     * 解析结算完成对应的账本账户
     */
    SettlementCompletedLedgerAccounts resolve(CurrencyCode currency);
}