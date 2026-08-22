package io.asteria.ledger.application.command;

import io.asteria.ledger.domain.enums.AccountCategory;
import io.asteria.ledger.domain.enums.AccountOwnerType;

import java.util.Currency;

public record CreateAccountCommand(
        String accountCode,
        AccountOwnerType ownerType,
        Long ownerId,
        AccountCategory category,
        Currency currency,
        boolean allowNegativeBalance
) {
}
