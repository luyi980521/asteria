package io.asteria.ledger.application.command;

import io.asteria.ledger.domain.enums.DebitCredit;
import io.asteria.ledger.domain.valueobject.LedgerAccountId;
import io.asteria.ledger.domain.valueobject.Money;

public record PostingCommand(
        LedgerAccountId ledgerAccountId,
        Money money,
        DebitCredit direction
) {
}
