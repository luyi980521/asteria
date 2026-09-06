package io.asteria.bootstrap;

import io.asteria.common.domain.valueobject.CurrencyCode;
import io.asteria.common.domain.valueobject.Money;
import io.asteria.common.trace.TraceConstants;
import io.asteria.common.trace.TraceScope;
import io.asteria.infrastructure.trace.KafkaTraceUtils;
import io.asteria.ledger.infrastructure.messaging.consumer.PaymentCapturedConsumer;
import io.asteria.ledger.infrastructure.messaging.consumer.SettlementCompletedConsumer;
import io.asteria.payment.application.assembler.PaymentOutboxEventAssembler;
import io.asteria.payment.application.publisher.PaymentMessagePublisher;
import io.asteria.payment.application.publisher.PaymentOutboxPublisher;
import io.asteria.payment.application.service.PaymentOutboxTransactionService;
import io.asteria.payment.application.task.PaymentOutboxPublishTask;
import io.asteria.payment.domain.entity.Payment;
import io.asteria.payment.domain.enums.PaymentMethod;
import io.asteria.payment.domain.enums.PaymentOutboxEventType;
import io.asteria.payment.domain.valueobject.PaymentId;
import io.asteria.payment.domain.valueobject.PaymentOutboxEvent;
import io.asteria.payment.domain.valueobject.PaymentReference;
import io.asteria.payment.infrastructure.persistence.converter.PaymentOutboxEventPersistenceConverter;
import io.asteria.settlement.application.assembler.SettlementOutboxEventAssembler;
import io.asteria.settlement.application.publisher.SettlementMessagePublisher;
import io.asteria.settlement.application.publisher.SettlementOutboxPublisher;
import io.asteria.settlement.application.service.SettlementOutboxTransactionService;
import io.asteria.settlement.application.task.SettlementOutboxPublishTask;
import io.asteria.settlement.domain.entity.SettlementBatch;
import io.asteria.settlement.domain.valueobject.SettlementBatchId;
import io.asteria.settlement.domain.valueobject.SettlementBatchReference;
import io.asteria.settlement.infrastructure.persistence.converter.SettlementOutboxEventPersistenceConverter;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.MDC;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Verifies persisted trace propagation across thread boundaries without Kafka or PostgreSQL.
 */
@SuppressWarnings("unchecked")
class TraceFlowTest {
    private static final String TRACE = "original-http-trace";
    private static final Instant NOW = Instant.parse("2026-09-06T00:00:00Z");
    private static final Money AMOUNT = Money.of(new BigDecimal("100.00"), CurrencyCode.of("USD"));

    @AfterEach
    void clear() {
        MDC.clear();
    }

    @Test
    void headersUseLastValueAndGenerateForMissingBlankOrNull() {
        var headers = new RecordHeaders();
        assertTrue(KafkaTraceUtils.resolveTraceId(headers).matches("[0-9a-f]{32}"));
        headers.add(TraceConstants.TRACE_ID_HEADER, "first".getBytes(StandardCharsets.UTF_8));
        headers.add(TraceConstants.TRACE_ID_HEADER, TRACE.getBytes(StandardCharsets.UTF_8));
        assertEquals(TRACE, KafkaTraceUtils.resolveTraceId(headers));
        headers.add(TraceConstants.TRACE_ID_HEADER, " ".getBytes(StandardCharsets.UTF_8));
        assertTrue(KafkaTraceUtils.resolveTraceId(headers).matches("[0-9a-f]{32}"));
        headers.add(TraceConstants.TRACE_ID_HEADER, null);
        assertTrue(KafkaTraceUtils.resolveTraceId(headers).matches("[0-9a-f]{32}"));
        assertNotEquals(KafkaTraceUtils.resolveTraceId(null), KafkaTraceUtils.resolveTraceId(null));
    }

    @Test
    void paymentTraceSurvivesPersistencePublishCallbackAndConsumption() throws Exception {
        Payment payment = Payment.create(PaymentId.of(1L), 42L, AMOUNT, PaymentMethod.CARD,
                new PaymentReference("ORDER", "order-1"), NOW);
        payment.startAuthorization();
        payment.authorize("auth-1", NOW);
        payment.startCapture();
        payment.capture(NOW);
        AtomicLong ids = new AtomicLong();
        MDC.put(TraceConstants.TRACE_ID, TRACE);
        var event = new PaymentOutboxEventAssembler(ids::incrementAndGet).toPaymentCapturedOutboxEvent(payment);
        assertEquals(TRACE, event.getTraceId());
        MDC.remove(TraceConstants.TRACE_ID);
        var converter = new PaymentOutboxEventPersistenceConverter();
        var restored = converter.toDomain(converter.toDO(event));
        assertEquals(TRACE, restored.getTraceId());
        assertEquals(event.getPayload(), restored.getPayload());

        KafkaTemplate<String, String> kafka = mock(KafkaTemplate.class);
        var transactions = mock(PaymentOutboxTransactionService.class);
        CompletableFuture<SendResult<String, String>> future = new CompletableFuture<>();
        when(kafka.send(any(ProducerRecord.class))).thenAnswer(invocation -> {
            assertEquals(TRACE, MDC.get(TraceConstants.TRACE_ID));
            return future;
        });
        doAnswer(invocation -> {
            assertEquals(TRACE, MDC.get(TraceConstants.TRACE_ID));
            return null;
        }).when(transactions).markPublished(eq(restored.getId()), any());
        MDC.put(TraceConstants.TRACE_ID, "scan-trace");
        new PaymentMessagePublisher(kafka, transactions).publish(restored);
        assertEquals("scan-trace", MDC.get(TraceConstants.TRACE_ID));
        verifyNoInteractions(transactions);
        ProducerRecord<String, String> record = sentRecord(kafka);
        assertEquals("payment-captured", record.topic());
        assertEquals(restored.getAggregateId(), record.key());
        assertEquals(restored.getPayload(), record.value());
        assertEquals(TRACE, KafkaTraceUtils.resolveTraceId(record.headers()));
        try (var executor = Executors.newSingleThreadExecutor()) {
            executor.submit(() -> {
                future.complete(null);
                assertNull(MDC.get(TraceConstants.TRACE_ID));
            }).get();
        }
        verify(transactions).markPublished(eq(restored.getId()), any());
        MDC.remove(TraceConstants.TRACE_ID);
        new PaymentCapturedConsumer(message -> {
            assertEquals(TRACE, MDC.get(TraceConstants.TRACE_ID));
            assertEquals(AMOUNT, message.amount());
        }).consume(received(record));
        assertNull(MDC.get(TraceConstants.TRACE_ID));
    }

    @Test
    void settlementTraceSurvivesPersistenceAndInlineCallback() {
        var batch = SettlementBatch.builder()
                .settlementBatchId(SettlementBatchId.builder().value(1L).build())
                .reference(SettlementBatchReference.builder().value("batch-1").build())
                .grossAmount(AMOUNT).feeAmount(AMOUNT).netAmount(AMOUNT).settledAt(NOW).build();
        AtomicLong ids = new AtomicLong();
        MDC.put(TraceConstants.TRACE_ID, TRACE);
        var event = new SettlementOutboxEventAssembler(ids::incrementAndGet).toSettlementCompletedOutboxEvent(batch);
        assertEquals(TRACE, event.getTraceId());
        MDC.remove(TraceConstants.TRACE_ID);
        var converter = new SettlementOutboxEventPersistenceConverter();
        var restored = converter.toDomain(converter.toDO(event));
        assertEquals(TRACE, restored.getTraceId());
        assertEquals(event.getPayload(), restored.getPayload());
        KafkaTemplate<String, String> kafka = mock(KafkaTemplate.class);
        var transactions = mock(SettlementOutboxTransactionService.class);
        when(kafka.send(any(ProducerRecord.class))).thenReturn(CompletableFuture.completedFuture(null));
        doAnswer(invocation -> {
            assertEquals(TRACE, MDC.get(TraceConstants.TRACE_ID));
            return null;
        }).when(transactions).markPublished(eq(restored.getId()), any());
        MDC.put(TraceConstants.TRACE_ID, "scan-trace");
        new SettlementMessagePublisher(kafka, transactions).publish(restored);
        assertEquals("scan-trace", MDC.get(TraceConstants.TRACE_ID));
        verify(transactions).markPublished(eq(restored.getId()), any());
        var record = sentRecord(kafka);
        assertEquals("settlement-completed", record.topic());
        assertEquals(event.getAggregateId(), record.key());
        assertEquals(event.getPayload(), record.value());
        assertEquals(TRACE, KafkaTraceUtils.resolveTraceId(record.headers()));
        MDC.remove(TraceConstants.TRACE_ID);
        new SettlementCompletedConsumer(message -> assertEquals(TRACE, MDC.get(TraceConstants.TRACE_ID)))
                .consume(received(record));
        assertNull(MDC.get(TraceConstants.TRACE_ID));
    }

    @Test
    void historicalOutboxGeneratesTraceAndFailureDoesNotMarkPublished() {
        KafkaTemplate<String, String> kafka = mock(KafkaTemplate.class);
        var transactions = mock(PaymentOutboxTransactionService.class);
        CompletableFuture<SendResult<String, String>> future = new CompletableFuture<>();
        when(kafka.send(any(ProducerRecord.class))).thenReturn(future);
        var event = PaymentOutboxEvent.builder().id(1L).aggregateId("1")
                .eventType(PaymentOutboxEventType.PAYMENT_CAPTURED).payload("{}").build();
        MDC.put(TraceConstants.TRACE_ID, "scan-trace");
        new PaymentMessagePublisher(kafka, transactions).publish(event);
        String trace = KafkaTraceUtils.resolveTraceId(sentRecord(kafka).headers());
        assertTrue(trace.matches("[0-9a-f]{32}"));
        assertNotEquals("scan-trace", trace);
        assertEquals("scan-trace", MDC.get(TraceConstants.TRACE_ID));
        future.completeExceptionally(new IllegalStateException("send failure"));
        assertEquals("scan-trace", MDC.get(TraceConstants.TRACE_ID));
        verifyNoInteractions(transactions);
    }

    @Test
    void consumersCleanMdcEvenWhenParsingOrDownstreamFails() {
        MDC.put("other", "kept");
        var malformed = new ConsumerRecord<String, String>("topic", 0, 0, "key", "invalid-json");
        assertThrows(IllegalArgumentException.class,
                () -> new PaymentCapturedConsumer(message -> fail()).consume(malformed));
        assertNull(MDC.get(TraceConstants.TRACE_ID));
        assertThrows(IllegalArgumentException.class,
                () -> new SettlementCompletedConsumer(message -> fail()).consume(malformed));
        assertNull(MDC.get(TraceConstants.TRACE_ID));
        var payload = new ConsumerRecord<String, String>("topic", 0, 0, "key", "{}");
        assertThrows(IllegalStateException.class, () -> new PaymentCapturedConsumer(message -> {
            assertTrue(MDC.get(TraceConstants.TRACE_ID).matches("[0-9a-f]{32}"));
            throw new IllegalStateException("business failure");
        }).consume(payload));
        assertNull(MDC.get(TraceConstants.TRACE_ID));
        assertEquals("kept", MDC.get("other"));
    }

    @Test
    void jobsGenerateScanTraceAndCleanOnFailure() {
        var payment = mock(PaymentOutboxPublisher.class);
        var settlement = mock(SettlementOutboxPublisher.class);
        doAnswer(invocation -> {
            assertTrue(MDC.get(TraceConstants.TRACE_ID).matches("[0-9a-f]{32}"));
            throw new IllegalStateException("scan failure");
        }).when(payment).publishPendingEvents();
        doAnswer(invocation -> {
            assertTrue(MDC.get(TraceConstants.TRACE_ID).matches("[0-9a-f]{32}"));
            return null;
        }).when(settlement).publishPendingEvents();
        new PaymentOutboxPublishTask(payment).publishPendingEvents();
        assertNull(MDC.get(TraceConstants.TRACE_ID));
        new SettlementOutboxPublishTask(settlement).publishPendingEvents();
        assertNull(MDC.get(TraceConstants.TRACE_ID));
    }

    @Test
    void nestedScopesRestoreOnlyTheirOwnKey() {
        MDC.put("other", "kept");
        try (var outer = TraceScope.open("outer")) {
            try (var inner = TraceScope.open("inner")) {
                assertEquals("inner", MDC.get(TraceConstants.TRACE_ID));
            }
            assertEquals("outer", MDC.get(TraceConstants.TRACE_ID));
        }
        assertNull(MDC.get(TraceConstants.TRACE_ID));
        assertEquals("kept", MDC.get("other"));
    }

    private ProducerRecord<String, String> sentRecord(KafkaTemplate<String, String> kafka) {
        ArgumentCaptor<ProducerRecord<String, String>> captor = ArgumentCaptor.forClass(ProducerRecord.class);
        verify(kafka).send(captor.capture());
        return captor.getValue();
    }

    private ConsumerRecord<String, String> received(ProducerRecord<String, String> sent) {
        var record = new ConsumerRecord<String, String>(sent.topic(), 0, 0, sent.key(), sent.value());
        sent.headers().forEach(header -> record.headers().add(header));
        return record;
    }
}
