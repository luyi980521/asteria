package io.asteria.bootstrap;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import io.asteria.balance.api.request.ConsumeBalanceRequest;
import io.asteria.balance.api.request.ReleaseBalanceRequest;
import io.asteria.balance.api.request.ReserveBalanceRequest;
import io.asteria.balance.application.assembler.BalanceRequestAssembler;
import io.asteria.balance.application.controller.BalanceController;
import io.asteria.balance.application.service.impl.BalanceApplicationServiceImpl;
import io.asteria.balance.domain.entity.Balance;
import io.asteria.balance.domain.entity.BalanceAccount;
import io.asteria.balance.domain.entity.BalanceMovement;
import io.asteria.balance.domain.entity.BalanceReservation;
import io.asteria.balance.domain.enums.BalanceAccountOwnerType;
import io.asteria.balance.domain.enums.BalanceMovementType;
import io.asteria.balance.domain.enums.BalanceReservationStatus;
import io.asteria.balance.domain.repository.BalanceAccountRepository;
import io.asteria.balance.domain.repository.BalanceMovementRepository;
import io.asteria.balance.domain.repository.BalanceRepository;
import io.asteria.balance.domain.repository.BalanceReservationRepository;
import io.asteria.balance.domain.valueobject.BalanceAccountId;
import io.asteria.balance.domain.valueobject.BalanceId;
import io.asteria.bootstrap.web.exception.GlobalExceptionHandler;
import io.asteria.common.application.port.DistributedIdGenerator;
import io.asteria.common.domain.valueobject.CurrencyCode;
import io.asteria.common.domain.valueobject.Money;
import io.asteria.payment.infrastructure.client.BalanceClient;
import io.asteria.payment.infrastructure.config.PaymentFeignConfiguration;
import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Real Feign proxy, HTTP serialization, controller and Balance rules; repositories are in-memory mocks. */
@SpringBootTest(classes = BalanceFeignHttpTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "asteria.service.balance.url=http://localhost:${local.server.port}",
        "spring.cloud.openfeign.lazy-attributes-resolution=true"
})
class BalanceFeignHttpTest {
    private static final Long MERCHANT_ID = 7L;
    private static final Long PAYMENT_ID = 1001L;
    private static final BalanceAccountId ACCOUNT_ID = BalanceAccountId.of(99L);
    private static final BalanceId BALANCE_ID = BalanceId.of(999L);
    private static final CurrencyCode USD = CurrencyCode.of("USD");

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class, MybatisPlusAutoConfiguration.class})
    @Import({PaymentFeignConfiguration.class, BalanceController.class, BalanceRequestAssembler.class,
            BalanceApplicationServiceImpl.class, GlobalExceptionHandler.class})
    static class TestApplication {
    }

    @Autowired private ApplicationContext context;
    @MockitoBean private BalanceAccountRepository accounts;
    @MockitoBean private BalanceRepository balances;
    @MockitoBean private BalanceReservationRepository reservations;
    @MockitoBean private BalanceMovementRepository movements;
    @MockitoBean private DistributedIdGenerator idGenerator;
    private Balance balance;
    private BalanceReservation reservation;

    @BeforeEach
    void setUp() {
        balance = Balance.create(BALANCE_ID, ACCOUNT_ID, USD);
        balance.credit(Money.of(new BigDecimal("100.00"), USD));
        reservation = null;
    }

    @Test
    void reserveThenConsumeUsesStoredAmountAndStableEventIdAcrossRetries() {
        existingAccount();
        BalanceClient client = context.getBean(BalanceClient.class);
        ReserveBalanceRequest reserve = new ReserveBalanceRequest(MERCHANT_ID, "USD", new BigDecimal("12.30"),
                "ORDER", "order-17", PAYMENT_ID);
        assertTrue(client.reserve(reserve).success());
        assertTrue(client.reserve(reserve).success());
        assertEquals(BalanceReservationStatus.RESERVED, reservation.getStatus());
        assertEquals(new BigDecimal("87.70"), balance.getAvailableAmount().amount());
        assertEquals(new BigDecimal("12.30"), balance.getReservedAmount().amount());

        ConsumeBalanceRequest consume = new ConsumeBalanceRequest(MERCHANT_ID, "USD", "ORDER", "order-17", PAYMENT_ID);
        assertTrue(client.consume(consume).success());
        assertTrue(client.consume(consume).success());
        assertEquals(BalanceReservationStatus.CONSUMED, reservation.getStatus());
        assertEquals(new BigDecimal("87.70"), balance.getAvailableAmount().amount());
        assertEquals(new BigDecimal("0.00"), balance.getReservedAmount().amount());
        assertMovements(BalanceMovementType.RESERVE, BalanceMovementType.DEBIT_RESERVED);
        verify(reservations, times(1)).save(any());
        verify(reservations, times(1)).update(reservation);
    }

    @Test
    void reserveThenReleaseRestoresAvailableAmountAndRetriesAreIdempotent() {
        existingAccount();
        BalanceClient client = context.getBean(BalanceClient.class);
        assertTrue(client.reserve(new ReserveBalanceRequest(MERCHANT_ID, "USD", new BigDecimal("12.30"),
                "ORDER", "order-17", PAYMENT_ID)).success());
        ReleaseBalanceRequest release = new ReleaseBalanceRequest(MERCHANT_ID, "USD", "ORDER", "order-17", PAYMENT_ID);
        assertTrue(client.release(release).success());
        assertTrue(client.release(release).success());
        assertEquals(BalanceReservationStatus.RELEASED, reservation.getStatus());
        assertEquals(new BigDecimal("100.00"), balance.getAvailableAmount().amount());
        assertEquals(new BigDecimal("0.00"), balance.getReservedAmount().amount());
        assertMovements(BalanceMovementType.RESERVE, BalanceMovementType.RELEASE);
    }

    @Test
    void missingMerchantAccountReturnsBalanceErrorEnvelope() {
        when(accounts.findByOwner(BalanceAccountOwnerType.MERCHANT, MERCHANT_ID)).thenReturn(Optional.empty());
        FeignException.BadRequest exception = assertThrows(FeignException.BadRequest.class,
                () -> context.getBean(BalanceClient.class).reserve(new ReserveBalanceRequest(
                        MERCHANT_ID, "USD", BigDecimal.TEN, "ORDER", "order-17", PAYMENT_ID)));
        assertTrue(exception.contentUTF8().contains("\"success\":false"));
        assertTrue(exception.contentUTF8().contains("BALANCE_0006"));
        verifyNoInteractions(balances, reservations, movements);
    }

    private void existingAccount() {
        when(accounts.findByOwner(BalanceAccountOwnerType.MERCHANT, MERCHANT_ID)).thenReturn(Optional.of(
                BalanceAccount.create(ACCOUNT_ID, BalanceAccountOwnerType.MERCHANT, MERCHANT_ID)));
        when(balances.findByBalanceAccountIdAndCurrency(ACCOUNT_ID, USD)).thenReturn(Optional.of(balance));
        when(reservations.findByBalanceIdAndReference(BALANCE_ID, "ORDER", "order-17"))
                .thenAnswer(invocation -> Optional.ofNullable(reservation));
        doAnswer(invocation -> {
            reservation = invocation.getArgument(0);
            return null;
        }).when(reservations).save(any());
        AtomicLong ids = new AtomicLong(2000L);
        when(idGenerator.nextId()).thenAnswer(invocation -> ids.incrementAndGet());
        Set<BalanceMovementType> saved = new HashSet<>();
        when(movements.existsByEventIdAndBalanceIdAndMovementType(eq(PAYMENT_ID.toString()), eq(BALANCE_ID), any()))
                .thenAnswer(invocation -> saved.contains(invocation.getArgument(2)));
        doAnswer(invocation -> {
            BalanceMovement movement = invocation.getArgument(0);
            saved.add(movement.getMovementType());
            return null;
        }).when(movements).save(any());
    }

    private void assertMovements(BalanceMovementType first, BalanceMovementType second) {
        var captor = ArgumentCaptor.forClass(BalanceMovement.class);
        verify(movements, times(2)).save(captor.capture());
        assertEquals(java.util.List.of(first, second),
                captor.getAllValues().stream().map(BalanceMovement::getMovementType).toList());
        for (BalanceMovement movement : captor.getAllValues()) {
            assertEquals(BALANCE_ID, movement.getBalanceId());
            assertEquals(PAYMENT_ID.toString(), movement.getEventId());
            assertEquals("ORDER", movement.getReferenceType());
            assertEquals("order-17", movement.getReferenceId());
            assertEquals(new BigDecimal("12.30"), movement.getAmount().amount());
            assertEquals(USD, movement.getAmount().currency());
        }
    }
}
