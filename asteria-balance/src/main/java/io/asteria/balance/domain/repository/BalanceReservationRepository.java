package io.asteria.balance.domain.repository;

import io.asteria.balance.domain.entity.BalanceReservation;
import io.asteria.balance.domain.valueobject.BalanceId;
import io.asteria.balance.domain.valueobject.BalanceReservationId;

import java.util.Optional;

public interface BalanceReservationRepository {
    Optional<BalanceReservation> findByReservationId(BalanceReservationId reservationId);

    Optional<BalanceReservation> findByBalanceIdAndReference(BalanceId balanceId, String referenceType,
                                                           String referenceId);

    void save(BalanceReservation reservation);

    void update(BalanceReservation reservation);
}
