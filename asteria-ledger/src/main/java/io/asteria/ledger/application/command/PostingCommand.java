package io.asteria.ledger.application.command;

import io.asteria.ledger.domain.enums.DebitCredit;
import io.asteria.ledger.domain.valueobject.AccountId;
import io.asteria.ledger.domain.valueobject.Money;

public record PostingCommand(
        AccountId accountId,
        Money money,
        DebitCredit direction
) {
}