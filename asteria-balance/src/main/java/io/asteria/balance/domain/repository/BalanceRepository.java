package io.asteria.balance.domain.repository;

import io.asteria.balance.domain.entity.Balance;
import io.asteria.balance.domain.valueobject.BalanceAccountId;
import io.asteria.balance.domain.valueobject.BalanceId;
import io.asteria.common.domain.valueobject.CurrencyCode;

import java.util.Optional;

public interface BalanceRepository {
    Optional<Balance> findByBalanceId(BalanceId balanceId);

    Optional<Balance> findByBalanceAccountIdAndCurrency(BalanceAccountId balanceAccountId, CurrencyCode currency);

    void save(Balance balance);

    /** 按读取时的版本更新；成功后再次更新前需重新查询数据库版本。 */
    void update(Balance balance);
}
