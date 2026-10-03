package io.asteria.balance.infrastructure.persistence.converter;

import io.asteria.balance.domain.entity.BalanceReservation;
import io.asteria.balance.domain.enums.BalanceReservationStatus;
import io.asteria.balance.domain.valueobject.BalanceReservationId;
import io.asteria.balance.domain.valueobject.BalanceId;
import io.asteria.common.domain.valueobject.CurrencyCode;
import io.asteria.common.domain.valueobject.Money;
import io.asteria.balance.infrastructure.persistence.dataobject.BalanceReservationDO;
import org.springframework.stereotype.Component;

@Component
public class BalanceReservationPersistenceConverter {
    public BalanceReservationDO toDataObject(BalanceReservation reservation) {
        return BalanceReservationDO.builder()
                .id(reservation.getReservationId().value())
                .balanceId(reservation.getBalanceId().value())
                .amount(reservation.getAmount().amount())
                .currency(reservation.getAmount().currency().value())
                .status(reservation.getStatus().name())
                .referenceType(reservation.getReferenceType())
                .referenceId(reservation.getReferenceId())
                .updatedAt(reservation.getUpdatedAt())
                .createdAt(reservation.getCreatedAt())
                .build();
    }

    public BalanceReservation toDomain(BalanceReservationDO dataObject) {
        return BalanceReservation.builder()
                .reservationId(BalanceReservationId.of(dataObject.getId()))
                .balanceId(BalanceId.of(dataObject.getBalanceId()))
                .amount(Money.of(dataObject.getAmount(), CurrencyCode.of(dataObject.getCurrency())))
                .status(BalanceReservationStatus.valueOf(dataObject.getStatus()))
                .referenceType(dataObject.getReferenceType())
                .referenceId(dataObject.getReferenceId())
                .updatedAt(dataObject.getUpdatedAt())
                .createdAt(dataObject.getCreatedAt())
                .build();
    }
}
