package io.asteria.ledger.application.request;

import io.asteria.ledger.domain.enums.AccountCategory;
import io.asteria.ledger.domain.enums.AccountOwnerType;

public record CreateAccountRequest(
        String accountCode,
        AccountOwnerType ownerType,
        Long ownerId,
        AccountCategory category,
        String currency,
        boolean allowNegativeBalance
) {
}
