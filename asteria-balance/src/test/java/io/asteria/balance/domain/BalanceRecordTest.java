package io.asteria.balance.domain;

import io.asteria.balance.domain.entity.BalanceMovement;
import io.asteria.balance.domain.entity.BalanceReservation;
import io.asteria.balance.domain.enums.BalanceMovementType;
import io.asteria.balance.domain.enums.BalanceReservationStatus;
import io.asteria.balance.domain.error.BalanceErrorCode;
import io.asteria.balance.domain.exception.BalanceDomainException;
import io.asteria.balance.domain.valueobject.BalanceAccountId;
import io.asteria.balance.domain.valueobject.BalanceId;
import io.asteria.balance.domain.valueobject.BalanceMovementId;
import io.asteria.balance.domain.valueobject.BalanceReservationId;
import io.asteria.common.domain.valueobject.CurrencyCode;
import io.asteria.common.domain.valueobject.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class BalanceRecordTest {
    private static final Instant CREATED = Instant.parse("2026-01-01T00:00:00Z");
    private static final Money AMOUNT = Money.of(BigDecimal.ONE, CurrencyCode.of("USD"));
    private static final Money ZERO = Money.ofNonNegative(BigDecimal.ZERO, CurrencyCode.of("USD"));

    @Test
    void idsValidateBothDirectConstructionAndFactories() {
        for (Long invalid : Arrays.asList(null, 0L, -1L)) {
            assertError(BalanceErrorCode.INVALID_PARAMS, () -> new BalanceId(invalid));
            assertError(BalanceErrorCode.INVALID_PARAMS, () -> BalanceId.of(invalid));
            assertError(BalanceErrorCode.INVALID_PARAMS, () -> new BalanceAccountId(invalid));
            assertError(BalanceErrorCode.INVALID_PARAMS, () -> BalanceAccountId.of(invalid));
            assertError(BalanceErrorCode.INVALID_PARAMS, () -> new BalanceMovementId(invalid));
            assertError(BalanceErrorCode.INVALID_PARAMS, () -> BalanceMovementId.of(invalid));
            assertError(BalanceErrorCode.INVALID_PARAMS, () -> new BalanceReservationId(invalid));
            assertError(BalanceErrorCode.INVALID_PARAMS, () -> BalanceReservationId.of(invalid));
        }
        assertEquals(new BalanceId(1L), BalanceId.of(1L));
    }

    @Test
    void movementRetainsAuditReferenceAndRejectsInvalidInput() {
        BalanceMovement movement = movementBuilder().build();
        assertEquals("event-1", movement.getEventId());
        assertEquals(AMOUNT, movement.getAmount());
        assertError(BalanceErrorCode.INVALID_PARAMS, () -> movementBuilder().eventId(" ").build());
        assertError(BalanceErrorCode.INVALID_PARAMS, () -> movementBuilder().balanceId(null).build());
        assertError(BalanceErrorCode.INVALID_PARAMS, () -> movementBuilder().movementType(null).build());
        assertError(BalanceErrorCode.INVALID_PARAMS, () -> movementBuilder().referenceId("").build());
        assertError(BalanceErrorCode.INVALID_AMOUNT, () -> movementBuilder().amount(null).build());
        assertError(BalanceErrorCode.INVALID_AMOUNT, () -> movementBuilder().amount(ZERO).build());
    }

    @Test
    void reservationCanRepresentEachStatusWithoutImplementingTransitions() {
        for (BalanceReservationStatus status : BalanceReservationStatus.values()) {
            assertEquals(status, reservationBuilder().status(status).build().getStatus());
        }
        assertError(BalanceErrorCode.INVALID_PARAMS, () -> reservationBuilder().status(null).build());
        assertError(BalanceErrorCode.INVALID_PARAMS, () -> reservationBuilder().updatedAt(null).build());
        assertError(BalanceErrorCode.INVALID_PARAMS, () -> reservationBuilder().referenceType(" ").build());
        assertError(BalanceErrorCode.INVALID_AMOUNT, () -> reservationBuilder().amount(null).build());
        assertError(BalanceErrorCode.INVALID_AMOUNT, () -> reservationBuilder().amount(ZERO).build());
    }

    private static BalanceMovement.BalanceMovementBuilder movementBuilder() {
        return BalanceMovement.builder().movementId(BalanceMovementId.of(1L)).balanceId(BalanceId.of(2L))
                .movementType(BalanceMovementType.CREDIT).amount(AMOUNT).referenceType("TEST")
                .referenceId("ref-1").eventId("event-1").createdAt(CREATED);
    }

    private static BalanceReservation.BalanceReservationBuilder reservationBuilder() {
        return BalanceReservation.builder().reservationId(BalanceReservationId.of(1L)).balanceId(BalanceId.of(2L))
                .amount(AMOUNT).status(BalanceReservationStatus.RESERVED).referenceType("TEST")
                .referenceId("ref-1").createdAt(CREATED).updatedAt(CREATED);
    }

    private static void assertError(BalanceErrorCode error, Runnable action) {
        assertEquals(error, assertThrows(BalanceDomainException.class, action::run).errorCode());
    }
}
