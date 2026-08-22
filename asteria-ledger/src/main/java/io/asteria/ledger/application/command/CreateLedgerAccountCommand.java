package io.asteria.ledger.application.command;

import io.asteria.ledger.domain.enums.LedgerAccountCategory;
import io.asteria.ledger.domain.enums.LedgerAccountOwnerType;

import java.util.Currency;

public record CreateLedgerAccountCommand(
        String accountCode,
        LedgerAccountOwnerType ownerType,
        Long ownerId,
        LedgerAccountCategory category,
        Currency currency,
        boolean allowNegativeBalance
) {
}
