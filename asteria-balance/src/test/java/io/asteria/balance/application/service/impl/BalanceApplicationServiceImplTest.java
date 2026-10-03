package io.asteria.balance.application.service.impl;

import io.asteria.balance.application.command.ConsumeBalanceCommand;
import io.asteria.balance.application.command.CreditBalanceCommand;
import io.asteria.balance.application.command.ReleaseBalanceCommand;
import io.asteria.balance.application.command.ReserveBalanceCommand;
import io.asteria.balance.domain.entity.Balance;
import io.asteria.balance.domain.entity.BalanceMovement;
import io.asteria.balance.domain.entity.BalanceReservation;
import io.asteria.balance.domain.enums.BalanceMovementType;
import io.asteria.balance.domain.enums.BalanceReservationStatus;
import io.asteria.balance.domain.error.BalanceErrorCode;
import io.asteria.balance.domain.exception.BalanceDomainException;
import io.asteria.balance.domain.repository.BalanceMovementRepository;
import io.asteria.balance.domain.repository.BalanceRepository;
import io.asteria.balance.domain.repository.BalanceReservationRepository;
import io.asteria.balance.domain.valueobject.BalanceAccountId;
import io.asteria.balance.domain.valueobject.BalanceId;
import io.asteria.balance.domain.valueobject.BalanceMovementId;
import io.asteria.balance.domain.valueobject.BalanceReservationId;
import io.asteria.common.application.port.DistributedIdGenerator;
import io.asteria.common.domain.valueobject.CurrencyCode;
import io.asteria.common.domain.valueobject.Money;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BalanceApplicationServiceImplTest {
    private static final BalanceId BALANCE_ID = BalanceId.of(1L);
    private static final BalanceAccountId ACCOUNT_ID = BalanceAccountId.of(2L);
    private static final BalanceReservationId RESERVATION_ID = BalanceReservationId.of(3L);
    private static final long MOVEMENT_ID = 4L;
    private static final CurrencyCode USD = CurrencyCode.of("USD");
    private static final String REFERENCE_TYPE = "PAYMENT";
    private static final String REFERENCE_ID = "payment-1";
    private static final String EVENT_ID = "event-1";
    private static final Instant CREATED = Instant.parse("2026-01-01T00:00:00Z");

    @Mock
    private BalanceRepository balanceRepository;
    @Mock
    private BalanceReservationRepository balanceReservationRepository;
    @Mock
    private BalanceMovementRepository balanceMovementRepository;
    @Mock
    private DistributedIdGenerator distributedIdGenerator;

    @InjectMocks
    private BalanceApplicationServiceImpl service;

    @Captor
    private ArgumentCaptor<BalanceMovement> movementCaptor;
    @Captor
    private ArgumentCaptor<BalanceReservation> reservationCaptor;

    @Test
    void credit_success() {
        Balance balance = balance("100", "0");
        stubBalance(balance);
        stubMovementExists(BalanceMovementType.CREDIT, false);
        when(distributedIdGenerator.nextId()).thenReturn(MOVEMENT_ID);

        service.credit(creditCommand());

        assertAmounts(balance, "130", "0");
        verify(balanceRepository).update(same(balance));
        assertSavedMovement(BalanceMovementType.CREDIT, money("30"));
        verify(distributedIdGenerator).nextId();
        verifyNoInteractions(balanceReservationRepository);
    }

    @Test
    void credit_duplicate_event_should_return() {
        Balance balance = balance("100", "0");
        stubBalance(balance);
        stubMovementExists(BalanceMovementType.CREDIT, true);

        assertDoesNotThrow(() -> service.credit(creditCommand()));

        assertUnchangedBalance(balance, "100", "0");
        verifyNoWrites();
        verifyNoInteractions(balanceReservationRepository);
    }

    @Test
    void reserve_success() {
        Balance balance = balance("100", "0");
        stubBalance(balance);
        stubMovementExists(BalanceMovementType.RESERVE, false);
        stubReservation(Optional.empty());
        when(distributedIdGenerator.nextId()).thenReturn(RESERVATION_ID.value(), MOVEMENT_ID);

        service.reserve(reserveCommand("30"));

        assertAmounts(balance, "70", "30");
        verify(balanceRepository).update(same(balance));
        verify(balanceReservationRepository).save(reservationCaptor.capture());
        BalanceReservation reservation = reservationCaptor.getValue();
        assertEquals(RESERVATION_ID, reservation.getReservationId());
        assertEquals(BALANCE_ID, reservation.getBalanceId());
        assertEquals(BalanceReservationStatus.RESERVED, reservation.getStatus());
        assertEquals(money("30"), reservation.getAmount());
        assertEquals(REFERENCE_TYPE, reservation.getReferenceType());
        assertEquals(REFERENCE_ID, reservation.getReferenceId());
        assertNotNull(reservation.getCreatedAt());
        assertEquals(reservation.getCreatedAt(), reservation.getUpdatedAt());
        assertSavedMovement(BalanceMovementType.RESERVE, money("30"));
        verify(distributedIdGenerator, times(2)).nextId();
        verify(balanceReservationRepository, never()).update(any());
        verify(balanceReservationRepository).findByBalanceIdAndReference(BALANCE_ID, REFERENCE_TYPE, REFERENCE_ID);
    }

    @Test
    void reserve_insufficient_available_balance() {
        Balance balance = balance("100", "0");
        stubBalance(balance);
        stubMovementExists(BalanceMovementType.RESERVE, false);
        stubReservation(Optional.empty());

        assertError(BalanceErrorCode.INSUFFICIENT_AVAILABLE_BALANCE,
                () -> service.reserve(reserveCommand("101")));

        assertUnchangedBalance(balance, "100", "0");
        verifyNoWrites();
    }

    @Test
    void reserve_duplicate_event_should_return() {
        Balance balance = balance("70", "30");
        stubBalance(balance);
        stubMovementExists(BalanceMovementType.RESERVE, true);

        assertDoesNotThrow(() -> service.reserve(reserveCommand("30")));

        assertUnchangedBalance(balance, "70", "30");
        verifyNoWrites();
        verifyNoInteractions(balanceReservationRepository);
    }

    @Test
    void reserve_same_reference_same_amount_should_return() {
        Balance balance = balance("70", "30");
        BalanceReservation reservation = reservation(BalanceReservationStatus.RESERVED, money("30"));
        stubBalance(balance);
        stubMovementExists(BalanceMovementType.RESERVE, false);
        stubReservation(Optional.of(reservation));

        assertDoesNotThrow(() -> service.reserve(reserveCommand("30")));

        assertUnchangedBalance(balance, "70", "30");
        assertUnchangedReservation(reservation, BalanceReservationStatus.RESERVED);
        verifyNoWrites();
        verify(balanceReservationRepository).findByBalanceIdAndReference(BALANCE_ID, REFERENCE_TYPE, REFERENCE_ID);
    }

    @Test
    void reserve_same_reference_different_amount_should_conflict() {
        Balance balance = balance("70", "30");
        BalanceReservation reservation = reservation(BalanceReservationStatus.RESERVED, money("30"));
        stubBalance(balance);
        stubMovementExists(BalanceMovementType.RESERVE, false);
        stubReservation(Optional.of(reservation));

        assertError(BalanceErrorCode.RESERVATION_REQUEST_CONFLICT, () -> service.reserve(reserveCommand("40")));

        assertUnchangedBalance(balance, "70", "30");
        assertUnchangedReservation(reservation, BalanceReservationStatus.RESERVED);
        verifyNoWrites();
    }

    @Test
    void reserve_same_reference_different_currency_should_conflict() {
        Balance balance = balance("70", "30");
        Money existingAmount = Money.of(new BigDecimal("30"), CurrencyCode.of("EUR"));
        BalanceReservation reservation = reservation(BalanceReservationStatus.RESERVED, existingAmount);
        stubBalance(balance);
        stubMovementExists(BalanceMovementType.RESERVE, false);
        stubReservation(Optional.of(reservation));

        assertError(BalanceErrorCode.RESERVATION_REQUEST_CONFLICT, () -> service.reserve(reserveCommand("30")));

        assertUnchangedBalance(balance, "70", "30");
        assertUnchangedReservation(reservation, BalanceReservationStatus.RESERVED);
        assertEquals(existingAmount, reservation.getAmount());
        verifyNoWrites();
    }

    @ParameterizedTest
    @EnumSource(value = BalanceReservationStatus.class, names = {"RELEASED", "CONSUMED"})
    void reserve_existing_terminal_reservation_should_fail(BalanceReservationStatus status) {
        Balance balance = balance("100", "0");
        BalanceReservation reservation = reservation(status, money("30"));
        stubBalance(balance);
        stubMovementExists(BalanceMovementType.RESERVE, false);
        stubReservation(Optional.of(reservation));

        assertError(BalanceErrorCode.RESERVATION_STATUS_INCORRECT, () -> service.reserve(reserveCommand("30")));

        assertUnchangedBalance(balance, "100", "0");
        assertUnchangedReservation(reservation, status);
        verifyNoWrites();
    }

    @Test
    void release_success() {
        Balance balance = balance("70", "30");
        BalanceReservation reservation = reservation(BalanceReservationStatus.RESERVED, money("30"));
        stubBalance(balance);
        stubMovementExists(BalanceMovementType.RELEASE, false);
        stubReservation(Optional.of(reservation));
        when(distributedIdGenerator.nextId()).thenReturn(MOVEMENT_ID);

        service.release(releaseCommand());

        assertAmounts(balance, "100", "0");
        assertEquals(BalanceReservationStatus.RELEASED, reservation.getStatus());
        verify(balanceRepository).update(same(balance));
        verify(balanceReservationRepository).update(same(reservation));
        verify(balanceReservationRepository, never()).save(any());
        assertSavedMovement(BalanceMovementType.RELEASE, reservation.getAmount());
        verify(distributedIdGenerator).nextId();
    }

    @Test
    void release_duplicate_event_should_return() {
        Balance balance = balance("100", "0");
        stubBalance(balance);
        stubMovementExists(BalanceMovementType.RELEASE, true);

        assertDoesNotThrow(() -> service.release(releaseCommand()));

        assertUnchangedBalance(balance, "100", "0");
        verifyNoWrites();
        verifyNoInteractions(balanceReservationRepository);
    }

    @Test
    void consume_success() {
        Balance balance = balance("70", "30");
        BalanceReservation reservation = reservation(BalanceReservationStatus.RESERVED, money("30"));
        stubBalance(balance);
        stubMovementExists(BalanceMovementType.DEBIT_RESERVED, false);
        stubReservation(Optional.of(reservation));
        when(distributedIdGenerator.nextId()).thenReturn(MOVEMENT_ID);

        service.consume(consumeCommand());

        assertAmounts(balance, "70", "0");
        assertEquals(BalanceReservationStatus.CONSUMED, reservation.getStatus());
        verify(balanceRepository).update(same(balance));
        verify(balanceReservationRepository).update(same(reservation));
        verify(balanceReservationRepository, never()).save(any());
        assertSavedMovement(BalanceMovementType.DEBIT_RESERVED, reservation.getAmount());
        verify(distributedIdGenerator).nextId();
    }

    @Test
    void consume_duplicate_event_should_return() {
        Balance balance = balance("70", "0");
        stubBalance(balance);
        stubMovementExists(BalanceMovementType.DEBIT_RESERVED, true);

        assertDoesNotThrow(() -> service.consume(consumeCommand()));

        assertUnchangedBalance(balance, "70", "0");
        verifyNoWrites();
        verifyNoInteractions(balanceReservationRepository);
    }

    @Test
    void release_consumed_reservation_should_fail() {
        Balance balance = balance("70", "0");
        BalanceReservation reservation = reservation(BalanceReservationStatus.CONSUMED, money("30"));
        stubBalance(balance);
        stubMovementExists(BalanceMovementType.RELEASE, false);
        stubReservation(Optional.of(reservation));

        assertError(BalanceErrorCode.INVALID_RESERVATION_STATE, () -> service.release(releaseCommand()));

        assertUnchangedBalance(balance, "70", "0");
        assertUnchangedReservation(reservation, BalanceReservationStatus.CONSUMED);
        verifyNoWrites();
    }

    @Test
    void consume_released_reservation_should_fail() {
        Balance balance = balance("100", "0");
        BalanceReservation reservation = reservation(BalanceReservationStatus.RELEASED, money("30"));
        stubBalance(balance);
        stubMovementExists(BalanceMovementType.DEBIT_RESERVED, false);
        stubReservation(Optional.of(reservation));

        assertError(BalanceErrorCode.INVALID_RESERVATION_STATE, () -> service.consume(consumeCommand()));

        assertUnchangedBalance(balance, "100", "0");
        assertUnchangedReservation(reservation, BalanceReservationStatus.RELEASED);
        verifyNoWrites();
    }

    private void stubBalance(Balance balance) {
        when(balanceRepository.findByBalanceAccountIdAndCurrency(ACCOUNT_ID, USD)).thenReturn(Optional.of(balance));
    }

    private void stubMovementExists(BalanceMovementType type, boolean exists) {
        when(balanceMovementRepository.existsByEventIdAndBalanceIdAndMovementType(EVENT_ID, BALANCE_ID, type))
                .thenReturn(exists);
    }

    private void stubReservation(Optional<BalanceReservation> reservation) {
        when(balanceReservationRepository.findByBalanceIdAndReference(BALANCE_ID, REFERENCE_TYPE, REFERENCE_ID))
                .thenReturn(reservation);
    }

    private void verifyNoWrites() {
        verify(balanceRepository, never()).save(any());
        verify(balanceRepository, never()).update(any());
        verify(balanceReservationRepository, never()).save(any());
        verify(balanceReservationRepository, never()).update(any());
        verify(balanceMovementRepository, never()).save(any());
        verifyNoInteractions(distributedIdGenerator);
    }

    private void assertSavedMovement(BalanceMovementType type, Money amount) {
        verify(balanceMovementRepository).save(movementCaptor.capture());
        BalanceMovement movement = movementCaptor.getValue();
        assertEquals(BalanceMovementId.of(MOVEMENT_ID), movement.getMovementId());
        assertEquals(BALANCE_ID, movement.getBalanceId());
        assertEquals(type, movement.getMovementType());
        assertEquals(amount, movement.getAmount());
        assertEquals(REFERENCE_TYPE, movement.getReferenceType());
        assertEquals(REFERENCE_ID, movement.getReferenceId());
        assertEquals(EVENT_ID, movement.getEventId());
        assertNotNull(movement.getCreatedAt());
    }

    private static void assertAmounts(Balance balance, String available, String reserved) {
        assertEquals(money(available), balance.getAvailableAmount());
        assertEquals(money(reserved), balance.getReservedAmount());
    }

    private static void assertUnchangedBalance(Balance balance, String available, String reserved) {
        assertAmounts(balance, available, reserved);
        assertEquals(CREATED, balance.getUpdatedAt());
        assertEquals(0L, balance.getVersion());
    }

    private static void assertUnchangedReservation(BalanceReservation reservation, BalanceReservationStatus status) {
        assertEquals(status, reservation.getStatus());
        assertEquals(CREATED, reservation.getUpdatedAt());
    }

    private static void assertError(BalanceErrorCode errorCode, Runnable action) {
        assertEquals(errorCode, assertThrows(BalanceDomainException.class, action::run).errorCode());
    }

    private static Balance balance(String available, String reserved) {
        return Balance.builder().balanceId(BALANCE_ID).balanceAccountId(ACCOUNT_ID).currency(USD)
                .availableAmount(money(available)).reservedAmount(money(reserved)).version(0L)
                .createdAt(CREATED).updatedAt(CREATED).build();
    }

    private static BalanceReservation reservation(BalanceReservationStatus status, Money amount) {
        return BalanceReservation.builder().reservationId(RESERVATION_ID).balanceId(BALANCE_ID)
                .status(status).amount(amount).referenceType(REFERENCE_TYPE).referenceId(REFERENCE_ID)
                .createdAt(CREATED).updatedAt(CREATED).build();
    }

    private static Money money(String amount) {
        return Money.ofNonNegative(new BigDecimal(amount), USD);
    }

    private static CreditBalanceCommand creditCommand() {
        return CreditBalanceCommand.builder().balanceAccountId(ACCOUNT_ID).currency(USD).amount(money("30"))
                .referenceType(REFERENCE_TYPE).referenceId(REFERENCE_ID).eventId(EVENT_ID).build();
    }

    private static ReserveBalanceCommand reserveCommand(String amount) {
        return ReserveBalanceCommand.builder().balanceAccountId(ACCOUNT_ID).currency(USD).amount(money(amount))
                .referenceType(REFERENCE_TYPE).referenceId(REFERENCE_ID).eventId(EVENT_ID).build();
    }

    private static ReleaseBalanceCommand releaseCommand() {
        return ReleaseBalanceCommand.builder().balanceAccountId(ACCOUNT_ID).currency(USD)
                .referenceType(REFERENCE_TYPE).referenceId(REFERENCE_ID).eventId(EVENT_ID).build();
    }

    private static ConsumeBalanceCommand consumeCommand() {
        return ConsumeBalanceCommand.builder().balanceAccountId(ACCOUNT_ID).currency(USD)
                .referenceType(REFERENCE_TYPE).referenceId(REFERENCE_ID).eventId(EVENT_ID).build();
    }
}
