package io.asteria.balance.application.controller;

import io.asteria.balance.api.request.ConsumeBalanceRequest;
import io.asteria.balance.api.request.ReleaseBalanceRequest;
import io.asteria.balance.api.request.ReserveBalanceRequest;
import io.asteria.balance.application.assembler.BalanceRequestAssembler;
import io.asteria.balance.application.command.ConsumeBalanceCommand;
import io.asteria.balance.application.command.ReleaseBalanceCommand;
import io.asteria.balance.application.command.ReserveBalanceCommand;
import io.asteria.balance.application.service.BalanceApplicationService;
import io.asteria.balance.domain.entity.BalanceAccount;
import io.asteria.balance.domain.enums.BalanceAccountOwnerType;
import io.asteria.balance.domain.error.BalanceErrorCode;
import io.asteria.balance.domain.exception.BalanceDomainException;
import io.asteria.balance.domain.repository.BalanceAccountRepository;
import io.asteria.balance.domain.valueobject.BalanceAccountId;
import io.asteria.common.domain.valueobject.CurrencyCode;
import io.asteria.common.domain.valueobject.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BalanceControllerTest {
    private static final Long MERCHANT_ID = 2L;
    private static final BalanceAccountId ACCOUNT_ID = BalanceAccountId.of(99L);
    private static final Long EVENT_ID = 1001L;
    private static final CurrencyCode CURRENCY = CurrencyCode.of("USD");
    @Mock private BalanceAccountRepository accounts;
    @Mock private BalanceApplicationService balances;
    private BalanceController controller;

    @BeforeEach
    void setUp() {
        controller = new BalanceController(balances, new BalanceRequestAssembler(accounts));
    }

    @Test
    void reserveResolvesMerchantAccountAndMapsAllFields() {
        existingAccount();
        var response = controller.reserve(new ReserveBalanceRequest(MERCHANT_ID, "usd", new BigDecimal("12.30"),
                "ORDER", "order-1", EVENT_ID));

        var command = ArgumentCaptor.forClass(ReserveBalanceCommand.class);
        verify(balances).reserve(command.capture());
        assertEquals(ACCOUNT_ID, command.getValue().getBalanceAccountId());
        assertNotEquals(MERCHANT_ID, command.getValue().getBalanceAccountId().value());
        assertEquals(CURRENCY, command.getValue().getCurrency());
        assertEquals(Money.of(new BigDecimal("12.30"), CURRENCY), command.getValue().getAmount());
        assertEquals("ORDER", command.getValue().getReferenceType());
        assertEquals("order-1", command.getValue().getReferenceId());
        assertEquals("1001", command.getValue().getEventId());
        assertTrue(response.success());
        assertNull(response.data());
        verify(accounts).findByOwner(BalanceAccountOwnerType.MERCHANT, MERCHANT_ID);
    }

    @Test
    void releaseMapsReferenceAndEventWithoutAcceptingAmount() {
        existingAccount();
        assertTrue(controller.release(new ReleaseBalanceRequest(MERCHANT_ID, "USD", "ORDER", "order-1", EVENT_ID))
                .success());

        var command = ArgumentCaptor.forClass(ReleaseBalanceCommand.class);
        verify(balances).release(command.capture());
        assertEquals(ACCOUNT_ID, command.getValue().getBalanceAccountId());
        assertEquals(CURRENCY, command.getValue().getCurrency());
        assertEquals("ORDER", command.getValue().getReferenceType());
        assertEquals("order-1", command.getValue().getReferenceId());
        assertEquals("1001", command.getValue().getEventId());
    }

    @Test
    void consumeMapsReferenceAndEventWithoutAcceptingAmount() {
        existingAccount();
        assertTrue(controller.consume(new ConsumeBalanceRequest(MERCHANT_ID, "USD", "ORDER", "order-1", EVENT_ID))
                .success());

        var command = ArgumentCaptor.forClass(ConsumeBalanceCommand.class);
        verify(balances).consume(command.capture());
        assertEquals(ACCOUNT_ID, command.getValue().getBalanceAccountId());
        assertEquals(CURRENCY, command.getValue().getCurrency());
        assertEquals("ORDER", command.getValue().getReferenceType());
        assertEquals("order-1", command.getValue().getReferenceId());
        assertEquals("1001", command.getValue().getEventId());
    }

    @Test
    void missingAccountFailsAllOperationsWithoutCreatingOrMutatingBalance() {
        when(accounts.findByOwner(BalanceAccountOwnerType.MERCHANT, MERCHANT_ID)).thenReturn(Optional.empty());
        assertInvalid(() -> controller.reserve(new ReserveBalanceRequest(MERCHANT_ID, "USD", BigDecimal.TEN,
                "ORDER", "order-1", EVENT_ID)));
        assertInvalid(() -> controller.release(new ReleaseBalanceRequest(MERCHANT_ID, "USD", "ORDER", "order-1", EVENT_ID)));
        assertInvalid(() -> controller.consume(new ConsumeBalanceRequest(MERCHANT_ID, "USD", "ORDER", "order-1", EVENT_ID)));
        verifyNoInteractions(balances);
        verify(accounts, never()).save(any());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(longs = {0L, -1L})
    void invalidMerchantIdFailsBeforeLookup(Long merchantId) {
        assertInvalid(() -> controller.reserve(new ReserveBalanceRequest(merchantId, "USD", BigDecimal.TEN,
                "ORDER", "order-1", EVENT_ID)));
        verifyNoInteractions(accounts, balances);
    }

    @Test
    void missingEventAndBlankReferenceFailBeforeLookup() {
        assertInvalid(() -> controller.release(new ReleaseBalanceRequest(MERCHANT_ID, "USD", "ORDER", "order-1", null)));
        assertInvalid(() -> controller.consume(new ConsumeBalanceRequest(MERCHANT_ID, "USD", " ", "order-1", EVENT_ID)));
        verifyNoInteractions(accounts, balances);
    }

    private void existingAccount() {
        when(accounts.findByOwner(BalanceAccountOwnerType.MERCHANT, MERCHANT_ID)).thenReturn(Optional.of(
                BalanceAccount.create(ACCOUNT_ID, BalanceAccountOwnerType.MERCHANT, MERCHANT_ID)));
    }

    private void assertInvalid(org.junit.jupiter.api.function.Executable operation) {
        assertEquals(BalanceErrorCode.INVALID_PARAMS,
                assertThrows(BalanceDomainException.class, operation).errorCode());
    }
}
