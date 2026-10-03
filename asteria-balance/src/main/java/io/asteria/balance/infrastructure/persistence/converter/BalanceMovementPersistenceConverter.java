package io.asteria.balance.infrastructure.persistence.converter;

import io.asteria.balance.domain.entity.BalanceMovement;
import io.asteria.balance.domain.enums.BalanceMovementType;
import io.asteria.balance.domain.valueobject.BalanceMovementId;
import io.asteria.balance.domain.valueobject.BalanceId;
import io.asteria.common.domain.valueobject.CurrencyCode;
import io.asteria.common.domain.valueobject.Money;
import io.asteria.balance.infrastructure.persistence.dataobject.BalanceMovementDO;
import org.springframework.stereotype.Component;

@Component
public class BalanceMovementPersistenceConverter {
    public BalanceMovementDO toDataObject(BalanceMovement movement) {
        return BalanceMovementDO.builder()
                .id(movement.getMovementId().value())
                .balanceId(movement.getBalanceId().value())
                .amount(movement.getAmount().amount())
                .currency(movement.getAmount().currency().value())
                .movementType(movement.getMovementType().name())
                .referenceType(movement.getReferenceType())
                .referenceId(movement.getReferenceId())
                .eventId(movement.getEventId())
                .createdAt(movement.getCreatedAt())
                .build();
    }

    public BalanceMovement toDomain(BalanceMovementDO dataObject) {
        return BalanceMovement.builder()
                .movementId(BalanceMovementId.of(dataObject.getId()))
                .balanceId(BalanceId.of(dataObject.getBalanceId()))
                .amount(Money.of(dataObject.getAmount(), CurrencyCode.of(dataObject.getCurrency())))
                .movementType(BalanceMovementType.valueOf(dataObject.getMovementType()))
                .referenceType(dataObject.getReferenceType())
                .referenceId(dataObject.getReferenceId())
                .eventId(dataObject.getEventId())
                .createdAt(dataObject.getCreatedAt())
                .build();
    }
}
