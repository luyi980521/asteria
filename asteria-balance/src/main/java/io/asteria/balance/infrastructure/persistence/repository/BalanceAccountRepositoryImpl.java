package io.asteria.balance.infrastructure.persistence.repository;

import io.asteria.balance.domain.entity.BalanceAccount;
import io.asteria.balance.domain.enums.BalanceAccountOwnerType;
import io.asteria.balance.domain.repository.BalanceAccountRepository;
import io.asteria.balance.domain.valueobject.BalanceAccountId;
import io.asteria.balance.infrastructure.persistence.converter.BalanceAccountPersistenceConverter;
import io.asteria.balance.infrastructure.persistence.mapper.BalanceAccountMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class BalanceAccountRepositoryImpl implements BalanceAccountRepository {
    private final BalanceAccountMapper mapper;
    private final BalanceAccountPersistenceConverter converter;

    @Override
    public Optional<BalanceAccount> findByBalanceAccountId(BalanceAccountId balanceAccountId) {
        return Optional.ofNullable(mapper.findByBalanceAccountId(balanceAccountId.value())).map(converter::toDomain);
    }

    @Override
    public Optional<BalanceAccount> findByOwner(BalanceAccountOwnerType ownerType, Long ownerId) {
        return Optional.ofNullable(mapper.findByOwner(ownerType.name(), ownerId)).map(converter::toDomain);
    }

    @Override
    public void save(BalanceAccount account) {
        mapper.insert(converter.toDataObject(account));
    }
}
