package io.asteria.ledger.application.model;

import io.asteria.ledger.domain.valueobject.LedgerAccountId;
import lombok.Builder;

/**
 * 支付捕获事件借贷账户
 * */
@Builder
public record PaymentCapturedLedgerAccounts(
        LedgerAccountId debitAccountId,
        LedgerAccountId creditAccountId
) {
}