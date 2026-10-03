package io.asteria.balance.infrastructure.persistence.repository;

import io.asteria.balance.domain.entity.BalanceReservation;
import io.asteria.balance.domain.error.BalanceErrorCode;
import io.asteria.balance.domain.exception.BalanceDomainException;
import io.asteria.balance.domain.repository.BalanceReservationRepository;
import io.asteria.balance.domain.valueobject.BalanceId;
import io.asteria.balance.domain.valueobject.BalanceReservationId;
import io.asteria.balance.infrastructure.persistence.converter.BalanceReservationPersistenceConverter;
import io.asteria.balance.infrastructure.persistence.mapper.BalanceReservationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class BalanceReservationRepositoryImpl implements BalanceReservationRepository {
    private final BalanceReservationMapper mapper;
    private final BalanceReservationPersistenceConverter converter;

    @Override
    public Optional<BalanceReservation> findByReservationId(BalanceReservationId reservationId) {
        return Optional.ofNullable(mapper.findByReservationId(reservationId.value())).map(converter::toDomain);
    }

    @Override
    public Optional<BalanceReservation> findByBalanceIdAndReference(BalanceId balanceId, String referenceType,
                                                                 String referenceId) {
        return Optional.ofNullable(mapper.findByBalanceIdAndReference(balanceId.value(), referenceType, referenceId))
                .map(converter::toDomain);
    }

    @Override
    public void save(BalanceReservation reservation) {
        mapper.insert(converter.toDataObject(reservation));
    }

    @Override
    public void update(BalanceReservation reservation) {
        if (mapper.updateStatus(converter.toDataObject(reservation)) == 0) {
            throw new BalanceDomainException(BalanceErrorCode.INVALID_PARAMS);
        }
    }
}
