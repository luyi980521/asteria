package io.asteria.ledger.application.request;

import io.asteria.ledger.domain.enums.LedgerAccountCategory;
import io.asteria.ledger.domain.enums.LedgerAccountOwnerType;

public record CreateLedgerAccountRequest(
        String accountCode,
        LedgerAccountOwnerType ownerType,
        Long ownerId,
        LedgerAccountCategory category,
        String currency,
        boolean allowNegativeBalance
) {
}
