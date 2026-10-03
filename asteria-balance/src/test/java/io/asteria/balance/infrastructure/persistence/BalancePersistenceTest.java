package io.asteria.balance.infrastructure.persistence;

import io.asteria.balance.domain.entity.Balance;
import io.asteria.balance.domain.entity.BalanceMovement;
import io.asteria.balance.domain.entity.BalanceReservation;
import io.asteria.balance.domain.enums.BalanceMovementType;
import io.asteria.balance.domain.enums.BalanceReservationStatus;
import io.asteria.balance.domain.valueobject.BalanceAccountId;
import io.asteria.balance.domain.valueobject.BalanceId;
import io.asteria.balance.domain.valueobject.BalanceMovementId;
import io.asteria.balance.domain.valueobject.BalanceReservationId;
import io.asteria.balance.infrastructure.persistence.converter.BalanceMovementPersistenceConverter;
import io.asteria.balance.infrastructure.persistence.converter.BalancePersistenceConverter;
import io.asteria.balance.infrastructure.persistence.converter.BalanceReservationPersistenceConverter;
import io.asteria.common.domain.valueobject.CurrencyCode;
import io.asteria.common.domain.valueobject.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class BalancePersistenceTest {
    private static final CurrencyCode USD = CurrencyCode.of("USD");
    private static final Instant CREATED = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void balanceRoundTripPreservesZeroAmountsAndDatabaseVersion() {
        Balance balance = Balance.builder().balanceId(BalanceId.of(1L))
                .balanceAccountId(BalanceAccountId.of(2L)).currency(USD)
                .availableAmount(Money.ofNonNegative(BigDecimal.ZERO, USD))
                .reservedAmount(Money.ofNonNegative(BigDecimal.ZERO, USD))
                .version(17L).createdAt(CREATED).updatedAt(CREATED).build();
        BalancePersistenceConverter converter = new BalancePersistenceConverter();
        Balance restored = converter.toDomain(converter.toDataObject(balance));
        assertEquals(balance.getBalanceId(), restored.getBalanceId());
        assertEquals(balance.getBalanceAccountId(), restored.getBalanceAccountId());
        assertEquals(USD, restored.getCurrency());
        assertEquals(balance.getAvailableAmount(), restored.getAvailableAmount());
        assertEquals(balance.getReservedAmount(), restored.getReservedAmount());
        assertEquals(17L, restored.getVersion());
        assertEquals(CREATED, restored.getCreatedAt());
        assertEquals(CREATED, restored.getUpdatedAt());
    }

    @Test
    void reservationRoundTripPreservesEachStatusAndReferences() {
        BalanceReservationPersistenceConverter converter = new BalanceReservationPersistenceConverter();
        for (BalanceReservationStatus status : BalanceReservationStatus.values()) {
            BalanceReservation reservation = BalanceReservation.builder().reservationId(BalanceReservationId.of(1L))
                    .balanceId(BalanceId.of(2L)).amount(Money.of(BigDecimal.ONE, USD)).status(status)
                    .referenceType("PAYMENT").referenceId("payment-1").createdAt(CREATED).updatedAt(CREATED).build();
            BalanceReservation restored = converter.toDomain(converter.toDataObject(reservation));
            assertEquals(reservation.getReservationId(), restored.getReservationId());
            assertEquals(reservation.getBalanceId(), restored.getBalanceId());
            assertEquals(reservation.getAmount(), restored.getAmount());
            assertEquals(status, restored.getStatus());
            assertEquals("PAYMENT", restored.getReferenceType());
            assertEquals("payment-1", restored.getReferenceId());
            assertEquals(CREATED, restored.getCreatedAt());
            assertEquals(CREATED, restored.getUpdatedAt());
        }
    }

    @Test
    void movementRoundTripPreservesEachTypeAndEventId() {
        BalanceMovementPersistenceConverter converter = new BalanceMovementPersistenceConverter();
        for (BalanceMovementType type : BalanceMovementType.values()) {
            BalanceMovement movement = BalanceMovement.builder().movementId(BalanceMovementId.of(1L))
                    .balanceId(BalanceId.of(2L)).amount(Money.of(BigDecimal.ONE, USD)).movementType(type)
                    .referenceType("PAYMENT").referenceId("payment-1").eventId("event-1").createdAt(CREATED).build();
            BalanceMovement restored = converter.toDomain(converter.toDataObject(movement));
            assertEquals(movement.getMovementId(), restored.getMovementId());
            assertEquals(movement.getBalanceId(), restored.getBalanceId());
            assertEquals(movement.getAmount(), restored.getAmount());
            assertEquals(type, restored.getMovementType());
            assertEquals("PAYMENT", restored.getReferenceType());
            assertEquals("payment-1", restored.getReferenceId());
            assertEquals("event-1", restored.getEventId());
            assertEquals(CREATED, restored.getCreatedAt());
        }
    }
}
