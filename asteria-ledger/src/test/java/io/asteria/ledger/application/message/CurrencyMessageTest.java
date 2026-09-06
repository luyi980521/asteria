package io.asteria.ledger.application.message;

import io.asteria.common.domain.valueobject.CurrencyCode;
import io.asteria.common.util.JsonUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CurrencyMessageTest {
    @Test
    void paymentConsumerReadsStringCurrency() {
        String json = """
                {"eventId":"event-1","paymentId":1,
                 "paymentReference":{"referenceType":"ORDER","referenceId":"payment-1"},
                 "amount":{"amount":100,"currency":"USD"},"capturedAt":"2026-09-06T00:00:00Z"}
                """;
        var message = JsonUtils.fromJson(json, PaymentCapturedMessage.class);
        assertEquals(CurrencyCode.of("USD"), message.amount().currency());
        assertEquals(message, JsonUtils.fromJson(JsonUtils.toJson(message), PaymentCapturedMessage.class));
    }

    @Test
    void settlementConsumerReadsAllMoneyFields() {
        String json = """
                {"eventId":"event-2","settlementBatchId":2,"settlementReference":"settlement-2",
                 "channelSettlementBatchId":"channel-2","grossAmount":{"amount":100,"currency":"USD"},
                 "feeAmount":{"amount":2,"currency":"USD"},"netAmount":{"amount":98,"currency":"USD"},
                 "settledAt":"2026-09-06T00:00:00Z"}
                """;
        var message = JsonUtils.fromJson(json, SettlementCompletedMessage.class);
        assertEquals(CurrencyCode.of("USD"), message.grossAmount().currency());
        assertEquals(CurrencyCode.of("USD"), message.feeAmount().currency());
        assertEquals(CurrencyCode.of("USD"), message.netAmount().currency());
        assertEquals(message, JsonUtils.fromJson(JsonUtils.toJson(message), SettlementCompletedMessage.class));
    }
}
