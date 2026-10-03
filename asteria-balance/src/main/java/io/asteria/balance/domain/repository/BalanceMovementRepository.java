package io.asteria.balance.domain.repository;

import io.asteria.balance.domain.entity.BalanceMovement;
import io.asteria.balance.domain.enums.BalanceMovementType;
import io.asteria.balance.domain.valueobject.BalanceId;

public interface BalanceMovementRepository {
    void save(BalanceMovement movement);

    boolean existsByEventIdAndBalanceIdAndMovementType(String eventId, BalanceId balanceId,
                                                     BalanceMovementType movementType);
}
