package io.asteria.balance.infrastructure.persistence.repository;

import io.asteria.balance.domain.entity.BalanceMovement;
import io.asteria.balance.domain.enums.BalanceMovementType;
import io.asteria.balance.domain.repository.BalanceMovementRepository;
import io.asteria.balance.domain.valueobject.BalanceId;
import io.asteria.balance.infrastructure.persistence.converter.BalanceMovementPersistenceConverter;
import io.asteria.balance.infrastructure.persistence.mapper.BalanceMovementMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class BalanceMovementRepositoryImpl implements BalanceMovementRepository {
    private final BalanceMovementMapper mapper;
    private final BalanceMovementPersistenceConverter converter;

    @Override
    public void save(BalanceMovement movement) {
        mapper.insert(converter.toDataObject(movement));
    }

    @Override
    public boolean existsByEventIdAndBalanceIdAndMovementType(String eventId, BalanceId balanceId,
                                                              BalanceMovementType movementType) {
        return mapper.existsByEventIdAndBalanceIdAndMovementType(eventId, balanceId.value(), movementType.name());
    }
}
