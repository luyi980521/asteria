package io.asteria.payment.entity;

import io.asteria.common.domain.valueobject.CurrencyCode;
import io.asteria.common.domain.valueobject.Money;
import io.asteria.common.util.JsonUtils;
import io.asteria.payment.domain.event.PaymentCapturedEvent;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PaymentCurrencyJsonTest {
    @Test
    void paymentEventRoundTripsWithStringCurrency() {
        var event = PaymentCapturedEvent.builder().eventId("event-1")
                .paymentId(1L).paymentReference(PaymentCapturedEvent.Reference.builder()
                        .referenceType("ORDER").referenceId("order-1").build())
                .amount(Money.of(new BigDecimal("100"), CurrencyCode.of("USD")))
                .capturedAt(Instant.parse("2026-09-06T00:00:00Z")).build();
        String json = JsonUtils.toJson(event);
        assertEquals(5, JsonUtils.readTree(json).size());
        assertEquals("USD", JsonUtils.readTree(json).path("amount").path("currency").asText());
        assertEquals(event, JsonUtils.fromJson(json, PaymentCapturedEvent.class));
    }
}
