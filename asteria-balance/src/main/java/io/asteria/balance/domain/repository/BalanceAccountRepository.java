package io.asteria.balance.domain.repository;

import io.asteria.balance.domain.entity.BalanceAccount;
import io.asteria.balance.domain.enums.BalanceAccountOwnerType;
import io.asteria.balance.domain.valueobject.BalanceAccountId;

import java.util.Optional;

public interface BalanceAccountRepository {
    Optional<BalanceAccount> findByBalanceAccountId(BalanceAccountId balanceAccountId);

    Optional<BalanceAccount> findByOwner(BalanceAccountOwnerType ownerType, Long ownerId);

    void save(BalanceAccount account);
}
