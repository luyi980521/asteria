package io.asteria.ledger.application.model;

import io.asteria.ledger.domain.valueobject.LedgerAccountId;
import lombok.Builder;

/**
 * 结算完成账户
 * */
@Builder
public record SettlementCompletedLedgerAccounts(
        LedgerAccountId cashAccountId,
        LedgerAccountId processingFeeExpenseAccountId,
        LedgerAccountId paymentReceivableAccountId
) {
}