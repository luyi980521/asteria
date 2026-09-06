package io.asteria.bootstrap;

import io.asteria.common.domain.valueobject.CurrencyCode;
import io.asteria.common.domain.valueobject.Money;
import io.asteria.common.util.JsonUtils;
import io.asteria.ledger.application.message.PaymentCapturedMessage;
import io.asteria.ledger.infrastructure.messaging.consumer.PaymentCapturedConsumer;
import io.asteria.payment.application.assembler.PaymentOutboxEventAssembler;
import io.asteria.payment.domain.entity.Payment;
import io.asteria.payment.domain.enums.PaymentMethod;
import io.asteria.payment.domain.event.PaymentCapturedEvent;
import io.asteria.payment.domain.valueobject.PaymentId;
import io.asteria.payment.domain.valueobject.PaymentReference;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/** Exercises both modules' actual boundary types without Kafka or a database. */
class PaymentCapturedContractTest {
    private static final Long PAYMENT_ID = 1001L;
    private static final Money AMOUNT = Money.of(new BigDecimal("100.00"), CurrencyCode.of("USD"));
    private static final Instant CAPTURED_AT = Instant.parse("2026-09-06T00:02:00Z");

    @Test
    void producerEventDeserializesDirectlyIntoConsumerMessage() {
        var event = PaymentCapturedEvent.builder()
                .eventId("event-1001")
                .paymentId(PAYMENT_ID)
                .paymentReference(PaymentCapturedEvent.Reference.builder()
                        .referenceType("ORDER").referenceId("order-1001").build())
                .amount(AMOUNT).capturedAt(CAPTURED_AT).build();

        String json = JsonUtils.toJson(event);
        PaymentCapturedMessage message = JsonUtils.fromJson(json, PaymentCapturedMessage.class);

        assertContract(json, "event-1001", message);
        assertEquals(JsonUtils.readTree(json), JsonUtils.readTree(JsonUtils.toJson(message)));
    }

    @Test
    void actualOutboxPayloadIsAcceptedByActualConsumer() {
        Payment payment = Payment.create(PaymentId.of(PAYMENT_ID), 42L, AMOUNT, PaymentMethod.CARD,
                new PaymentReference("ORDER", "order-1001"), CAPTURED_AT.minusSeconds(120));
        payment.startAuthorization();
        payment.authorize("authorization-1001", CAPTURED_AT.minusSeconds(60));
        payment.startCapture();
        payment.capture(CAPTURED_AT);
        AtomicLong ids = new AtomicLong(2000);
        var assembler = new PaymentOutboxEventAssembler(ids::incrementAndGet);
        var outbox = assembler.toPaymentCapturedOutboxEvent(payment);
        AtomicReference<PaymentCapturedMessage> delivered = new AtomicReference<>();
        var consumer = new PaymentCapturedConsumer(delivered::set);

        consumer.consume(outbox.getPayload());

        assertContract(outbox.getPayload(), outbox.getEventId(), delivered.get());
        assertEquals("2001", outbox.getEventId());
        assertEquals(PAYMENT_ID, payment.getPaymentId().value());
        assertEquals("ORDER", payment.getReference().referenceType());
        assertEquals("order-1001", payment.getReference().referenceId());
    }

    private void assertContract(String json, String eventId, PaymentCapturedMessage message) {
        assertNotNull(message);
        assertEquals(eventId, message.eventId());
        assertEquals(PAYMENT_ID, message.paymentId());
        assertEquals("ORDER", message.paymentReference().referenceType());
        assertEquals("order-1001", message.paymentReference().referenceId());
        assertEquals(AMOUNT, message.amount());
        assertEquals(CurrencyCode.of("USD"), message.amount().currency());
        assertEquals(CAPTURED_AT, message.capturedAt());

        var tree = JsonUtils.readTree(json);
        assertEquals(5, tree.size());
        assertTrue(tree.path("eventId").isTextual());
        assertTrue(tree.path("paymentId").isIntegralNumber());
        assertEquals(PAYMENT_ID.longValue(), tree.path("paymentId").longValue());
        assertEquals(2, tree.path("paymentReference").size());
        assertEquals(2, tree.path("amount").size());
        assertEquals(0, new BigDecimal("100.00").compareTo(tree.path("amount").path("amount").decimalValue()));
        assertTrue(tree.path("amount").path("currency").isTextual());
        assertEquals("USD", tree.path("amount").path("currency").textValue());
    }
}
