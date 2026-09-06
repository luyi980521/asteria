package io.asteria.ledger.application.command;

import io.asteria.common.domain.valueobject.CurrencyCode;
import io.asteria.ledger.domain.enums.LedgerAccountCategory;
import io.asteria.ledger.domain.enums.LedgerAccountOwnerType;


public record CreateLedgerAccountCommand(
        String accountCode,
        LedgerAccountOwnerType ownerType,
        Long ownerId,
        LedgerAccountCategory category,
        CurrencyCode currency,
        boolean allowNegativeBalance
) {
}
