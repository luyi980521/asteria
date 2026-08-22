package io.asteria.ledger.domain.repository;

import io.asteria.ledger.domain.entity.LedgerAccount;
import io.asteria.ledger.domain.valueobject.LedgerAccountId;

public interface LedgerAccountRepository {

    void insert(LedgerAccount ledgerAccount);

    void updateByAccountCode(LedgerAccount ledgerAccount);

    LedgerAccount findById(LedgerAccountId ledgerAccountId);

    LedgerAccount findByAccountCode(String accountCode);

    boolean existsByAccountCode(String accountCode);
}
