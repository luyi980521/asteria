package io.asteria.payment.application.service.impl;

import io.asteria.balance.api.request.ConsumeBalanceRequest;
import io.asteria.balance.api.request.ReleaseBalanceRequest;
import io.asteria.balance.api.request.ReserveBalanceRequest;
import io.asteria.common.application.exception.RemoteServiceException;
import io.asteria.common.application.port.DistributedIdGenerator;
import io.asteria.common.domain.valueobject.CurrencyCode;
import io.asteria.common.domain.valueobject.Money;
import io.asteria.payment.application.assembler.PaymentOutboxEventAssembler;
import io.asteria.payment.application.port.channel.*;
import io.asteria.payment.application.port.router.PaymentChannelRouter;
import io.asteria.payment.domain.entity.Payment;
import io.asteria.payment.domain.enums.PaymentMethod;
import io.asteria.payment.domain.enums.PaymentOutboxEventType;
import io.asteria.payment.domain.enums.PaymentStatus;
import io.asteria.payment.domain.repository.PaymentOutboxEventRepository;
import io.asteria.payment.domain.repository.PaymentRepository;
import io.asteria.payment.domain.valueobject.PaymentId;
import io.asteria.payment.domain.valueobject.PaymentOutboxEvent;
import io.asteria.payment.domain.valueobject.PaymentReference;
import io.asteria.payment.infrastructure.client.BalanceClient;
import io.asteria.payment.infrastructure.client.LedgerClient;
import io.asteria.web.response.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentApplicationServiceImplTest {
    private static final PaymentId PAYMENT_ID = PaymentId.of(1001L);
    private static final Long MERCHANT_ID = 2002L;
    private static final Money AMOUNT = Money.of(new BigDecimal("125.00"), CurrencyCode.of("USD"));

    @Mock private PaymentRepository paymentRepository;
    @Mock private DistributedIdGenerator idGenerator;
    @Mock private PaymentChannelRouter router;
    @Mock private PaymentChannel channel;
    @Mock private LedgerClient ledgerClient;
    @Mock private BalanceClient balanceClient;
    @Mock private PaymentOutboxEventRepository outboxRepository;
    private PaymentTransactionServiceImpl transactions;
    private PaymentApplicationServiceImpl service;
    private Payment payment;

    @BeforeEach
    void setUp() {
        payment = Payment.create(PAYMENT_ID, MERCHANT_ID, AMOUNT, PaymentMethod.CARD,
                new PaymentReference("ORDER", "order-17"), Instant.now());
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(payment);
        transactions = spy(new PaymentTransactionServiceImpl(paymentRepository,
                new PaymentOutboxEventAssembler(idGenerator), outboxRepository));
        service = new PaymentApplicationServiceImpl(paymentRepository, idGenerator, router,
                transactions, channel, ledgerClient, balanceClient, outboxRepository);
    }

    @Test
    void authorizeSuccessReservesBeforeChannelAndDoesNotRelease() {
        prepareAuthorization(AuthorizationResult.success("auth-1"));

        service.authorize(PAYMENT_ID);

        var request = ArgumentCaptor.forClass(ReserveBalanceRequest.class);
        var order = inOrder(transactions, balanceClient, channel);
        order.verify(transactions).startAuthorization(PAYMENT_ID);
        order.verify(balanceClient).reserve(request.capture());
        order.verify(channel).authorize(any());
        order.verify(transactions).completeAuthorization(PAYMENT_ID, AuthorizationResult.success("auth-1"));
        assertEquals(new ReserveBalanceRequest(MERCHANT_ID, "USD", AMOUNT.amount(),
                "ORDER", "order-17", PAYMENT_ID.value()), request.getValue());
        assertEquals(PaymentStatus.AUTHORIZED, payment.getStatus());
        verify(balanceClient, never()).release(any());
        verify(balanceClient, never()).consume(any());
        verifyNoInteractions(idGenerator, ledgerClient, outboxRepository);
    }

    @Test
    void authorizeDefiniteFailureReleasesBeforeCompletingFailure() {
        AuthorizationResult failure = AuthorizationResult.failure("DECLINED", "Declined");
        prepareAuthorization(failure);

        service.authorize(PAYMENT_ID);

        var request = ArgumentCaptor.forClass(ReleaseBalanceRequest.class);
        var order = inOrder(balanceClient, channel, transactions);
        order.verify(balanceClient).reserve(any());
        order.verify(channel).authorize(any());
        order.verify(balanceClient).release(request.capture());
        order.verify(transactions).completeAuthorization(PAYMENT_ID, failure);
        assertEquals(new ReleaseBalanceRequest(MERCHANT_ID, "USD", "ORDER", "order-17", PAYMENT_ID.value()),
                request.getValue());
        assertEquals(PaymentStatus.FAILED, payment.getStatus());
        verifyNoInteractions(idGenerator, ledgerClient, outboxRepository);
    }

    @Test
    void authorizeUnknownKeepsReservationAndInFlightState() {
        prepareAuthorization(AuthorizationResult.unknown());

        service.authorize(PAYMENT_ID);

        verify(balanceClient).reserve(any());
        verify(balanceClient, never()).release(any());
        verify(transactions, never()).completeAuthorization(any(), any());
        assertEquals(PaymentStatus.AUTHORIZING, payment.getStatus());
    }

    @ParameterizedTest
    @ValueSource(strings = {"UNKNOWN", "TIMEOUT"})
    void authorizeIndeterminateFailureCodeDoesNotRelease(String failureCode) {
        prepareAuthorization(AuthorizationResult.failure(failureCode, "No definitive result"));

        service.authorize(PAYMENT_ID);

        verify(balanceClient).reserve(any());
        verify(balanceClient, never()).release(any());
        verify(transactions, never()).completeAuthorization(any(), any());
        assertEquals(PaymentStatus.AUTHORIZING, payment.getStatus());
    }

    @Test
    void authorizeExceptionDoesNotRelease() {
        when(balanceClient.reserve(any())).thenReturn(ApiResponse.successWithoutData());
        when(router.route(payment)).thenReturn(channel);
        RuntimeException timeout = new RuntimeException("Channel timeout");
        when(channel.authorize(any())).thenThrow(timeout);

        assertSame(timeout, assertThrows(RuntimeException.class, () -> service.authorize(PAYMENT_ID)));

        verify(balanceClient).reserve(any());
        verify(balanceClient, never()).release(any());
        verify(transactions, never()).completeAuthorization(any(), any());
        assertEquals(PaymentStatus.AUTHORIZING, payment.getStatus());
    }

    @Test
    void reserveFailureStopsBeforeChannel() {
        when(balanceClient.reserve(any())).thenReturn(ApiResponse.failure("BALANCE_0003", "Insufficient balance"));

        assertThrows(RemoteServiceException.class, () -> service.authorize(PAYMENT_ID));

        verifyNoInteractions(channel, router);
        verify(transactions, never()).completeAuthorization(any(), any());
    }

    @Test
    void releaseFailureDoesNotCompleteAuthorization() {
        prepareAuthorization(AuthorizationResult.failure("DECLINED", "Declined"));
        when(balanceClient.release(any())).thenReturn(ApiResponse.failure("SYSTEM_ERROR", "Unavailable"));

        assertThrows(RemoteServiceException.class, () -> service.authorize(PAYMENT_ID));

        verify(transactions, never()).completeAuthorization(any(), any());
        assertEquals(PaymentStatus.AUTHORIZING, payment.getStatus());
    }

    @Test
    void captureSuccessConsumesBeforeCompletionAndWritesExistingOutbox() {
        prepareCapture(CaptureResult.success("capture-1"));
        when(balanceClient.consume(any())).thenReturn(ApiResponse.successWithoutData());
        when(idGenerator.nextId()).thenReturn(3003L, 4004L);

        service.capture(PAYMENT_ID);

        var request = ArgumentCaptor.forClass(ConsumeBalanceRequest.class);
        var order = inOrder(transactions, channel, balanceClient, outboxRepository);
        order.verify(transactions).startCapture(PAYMENT_ID);
        order.verify(channel).capture(any());
        order.verify(balanceClient).consume(request.capture());
        order.verify(transactions).completeCapture(PAYMENT_ID, CaptureResult.success("capture-1"));
        var event = ArgumentCaptor.forClass(PaymentOutboxEvent.class);
        order.verify(outboxRepository).insert(event.capture());
        assertEquals(new ConsumeBalanceRequest(MERCHANT_ID, "USD", "ORDER", "order-17", PAYMENT_ID.value()),
                request.getValue());
        assertEquals(PaymentStatus.CAPTURED, payment.getStatus());
        assertEquals(PaymentOutboxEventType.PAYMENT_CAPTURED, event.getValue().getEventType());
        assertEquals("1001", event.getValue().getAggregateId());
        verify(balanceClient, never()).release(any());
        verifyNoInteractions(ledgerClient);
    }

    @Test
    void captureFailureReturnsToAuthorizedWithoutBalanceChanges() {
        CaptureResult failure = CaptureResult.failure("DECLINED", "Declined");
        prepareCapture(failure);

        service.capture(PAYMENT_ID);

        verify(transactions).completeCapture(PAYMENT_ID, failure);
        assertEquals(PaymentStatus.AUTHORIZED, payment.getStatus());
        verifyNoInteractions(balanceClient, outboxRepository, ledgerClient, idGenerator);
    }

    @Test
    void captureUnknownKeepsReservationAndCapturingState() {
        prepareCapture(CaptureResult.unknown());

        service.capture(PAYMENT_ID);

        assertEquals(PaymentStatus.CAPTURING, payment.getStatus());
        verify(transactions, never()).completeCapture(any(), any());
        verifyNoInteractions(balanceClient, outboxRepository);
    }

    @ParameterizedTest
    @ValueSource(strings = {"UNKNOWN", "TIMEOUT"})
    void captureIndeterminateFailureCodeRetainsCapturingState(String failureCode) {
        prepareCapture(CaptureResult.failure(failureCode, "No definitive result"));

        service.capture(PAYMENT_ID);

        assertEquals(PaymentStatus.CAPTURING, payment.getStatus());
        verify(transactions, never()).completeCapture(any(), any());
        verifyNoInteractions(balanceClient, outboxRepository);
    }

    @Test
    void captureExceptionDoesNotChangeBalanceOrCompleteCapture() {
        authorizeLocally();
        when(router.route(payment)).thenReturn(channel);
        when(channel.capture(any())).thenThrow(new RuntimeException("Timeout"));

        assertThrows(RuntimeException.class, () -> service.capture(PAYMENT_ID));

        assertEquals(PaymentStatus.CAPTURING, payment.getStatus());
        verify(transactions, never()).completeCapture(any(), any());
        verifyNoInteractions(balanceClient, outboxRepository);
    }

    @Test
    void consumeFailureStopsCompletionAndOutbox() {
        prepareCapture(CaptureResult.success("capture-1"));
        when(balanceClient.consume(any())).thenReturn(ApiResponse.failure("SYSTEM_ERROR", "Unavailable"));

        assertThrows(RemoteServiceException.class, () -> service.capture(PAYMENT_ID));

        assertEquals(PaymentStatus.CAPTURING, payment.getStatus());
        verify(transactions, never()).completeCapture(any(), any());
        verifyNoInteractions(outboxRepository);
        verify(balanceClient, never()).release(any());
    }

    @Test
    void recoverSuccessfulCaptureConsumesUsingSamePaymentIdBeforeCompletion() {
        authorizeLocally();
        payment.startCapture();
        when(channel.queryCapture(payment)).thenReturn(CaptureQueryResult.success("capture-1"));
        when(balanceClient.consume(any())).thenReturn(ApiResponse.successWithoutData());
        when(idGenerator.nextId()).thenReturn(3003L, 4004L);

        service.recoverCapture(PAYMENT_ID);

        var order = inOrder(balanceClient, transactions);
        order.verify(balanceClient).consume(new ConsumeBalanceRequest(MERCHANT_ID, "USD", "ORDER", "order-17",
                PAYMENT_ID.value()));
        order.verify(transactions).completeCapture(PAYMENT_ID, CaptureResult.success("capture-1"));
        assertEquals(PaymentStatus.CAPTURED, payment.getStatus());
        verify(outboxRepository).insert(any());
    }

    private void prepareAuthorization(AuthorizationResult result) {
        when(balanceClient.reserve(any())).thenReturn(ApiResponse.successWithoutData());
        if (!result.success() && !result.isUnknown()) {
            when(balanceClient.release(any())).thenReturn(ApiResponse.successWithoutData());
        }
        when(router.route(payment)).thenReturn(channel);
        when(channel.authorize(any())).thenReturn(result);
    }

    private void prepareCapture(CaptureResult result) {
        authorizeLocally();
        when(router.route(payment)).thenReturn(channel);
        when(channel.capture(any())).thenReturn(result);
    }

    private void authorizeLocally() {
        payment.startAuthorization();
        payment.authorize("auth-1", Instant.now());
    }
}
