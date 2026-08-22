package io.asteria.ledger.domain.repository;

import io.asteria.ledger.domain.entity.Account;
import io.asteria.ledger.domain.valueobject.AccountId;

public interface AccountRepository {

    void insert(Account account);

    void updateByAccountCode(Account account);

    Account findById(AccountId accountId);

    Account findByAccountCode(String accountCode);

    boolean existsByAccountCode(String accountCode);
}