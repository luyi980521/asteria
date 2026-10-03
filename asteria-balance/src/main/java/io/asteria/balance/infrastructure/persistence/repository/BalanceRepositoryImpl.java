package io.asteria.balance.infrastructure.persistence.repository;

import io.asteria.balance.domain.entity.Balance;
import io.asteria.balance.domain.error.BalanceErrorCode;
import io.asteria.balance.domain.exception.BalanceDomainException;
import io.asteria.balance.domain.repository.BalanceRepository;
import io.asteria.balance.domain.valueobject.BalanceAccountId;
import io.asteria.balance.domain.valueobject.BalanceId;
import io.asteria.balance.infrastructure.persistence.converter.BalancePersistenceConverter;
import io.asteria.balance.infrastructure.persistence.mapper.BalanceMapper;
import io.asteria.common.domain.valueobject.CurrencyCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class BalanceRepositoryImpl implements BalanceRepository {
    private final BalanceMapper mapper;
    private final BalancePersistenceConverter converter;

    @Override
    public Optional<Balance> findByBalanceId(BalanceId balanceId) {
        return Optional.ofNullable(mapper.findByBalanceId(balanceId.value())).map(converter::toDomain);
    }

    @Override
    public Optional<Balance> findByBalanceAccountIdAndCurrency(BalanceAccountId balanceAccountId,
                                                               CurrencyCode currency) {
        return Optional.ofNullable(mapper.findByBalanceAccountIdAndCurrency(balanceAccountId.value(), currency.value()))
                .map(converter::toDomain);
    }

    @Override
    public void save(Balance balance) {
        mapper.insert(converter.toDataObject(balance));
    }

    @Override
    public void update(Balance balance) {
        if (mapper.updateWithVersion(converter.toDataObject(balance)) == 0) {
            throw new BalanceDomainException(BalanceErrorCode.BALANCE_CONCURRENT_MODIFICATION);
        }
    }
}
