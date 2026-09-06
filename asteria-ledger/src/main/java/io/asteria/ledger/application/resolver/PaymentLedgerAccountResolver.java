package io.asteria.ledger.application.resolver;

import io.asteria.common.domain.valueobject.CurrencyCode;
import io.asteria.ledger.application.model.PaymentCapturedLedgerAccounts;

/**
 * Payment 记账账户解析器
 */
public interface PaymentLedgerAccountResolver {

    /**
     * 解析支付捕获成功对应的账本账户
     */
    PaymentCapturedLedgerAccounts resolve(CurrencyCode currency);
}