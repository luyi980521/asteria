package io.asteria.settlement;

import io.asteria.common.domain.valueobject.CurrencyCode;
import io.asteria.common.domain.valueobject.Money;
import io.asteria.common.util.JsonUtils;
import io.asteria.settlement.domain.entity.SettlementBatch;
import io.asteria.settlement.domain.entity.SettlementItem;
import io.asteria.settlement.domain.event.SettlementCompletedEvent;
import io.asteria.settlement.domain.valueobject.SettlementBatchId;
import io.asteria.settlement.domain.valueobject.SettlementBatchReference;
import io.asteria.settlement.domain.valueobject.SettlementItemId;
import io.asteria.settlement.infrastructure.persistence.converter.SettlementPersistenceConverter;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CurrencyPersistenceTest {
    @Test
    void batchAndItemPersistCurrencyCodeInsteadOfSymbol() {
        var currency = CurrencyCode.of("USD");
        var gross = Money.of(new BigDecimal("100"), currency);
        var fee = Money.of(new BigDecimal("2"), currency);
        var net = Money.of(new BigDecimal("98"), currency);
        var now = Instant.parse("2026-09-06T00:00:00Z");
        var item = SettlementItem.builder().settlementItemId(SettlementItemId.builder().value(1L).build())
                .paymentId(1L).paymentReference("payment-1").amount(gross).createdAt(now).build();
        var batch = SettlementBatch.create(SettlementBatchId.builder().value(1L).build(),
                SettlementBatchReference.builder().value("batch-1").build(), gross, fee, net, List.of(item), now);
        var converter = new SettlementPersistenceConverter();
        var batchDO = converter.toBatchDO(batch);
        var itemDO = converter.toItemDO(batch.getSettlementBatchId(), item);
        assertEquals(currency, batch.getCurrency());
        assertEquals("USD", batchDO.getCurrency());
        assertEquals("USD", itemDO.getCurrency());
        var restored = converter.toDomain(batchDO, List.of(itemDO));
        assertEquals(currency, restored.getCurrency());
        assertEquals(gross, restored.getGrossAmount());
        assertEquals(fee, restored.getFeeAmount());
        assertEquals(net, restored.getNetAmount());
        assertEquals(gross, restored.getItems().getFirst().getAmount());

        var event = SettlementCompletedEvent.builder().eventId("event-1").settlementBatchId(1L)
                .settlementReference("batch-1").channelSettlementBatchId("channel-1")
                .grossAmount(gross).feeAmount(fee).netAmount(net).settledAt(now).build();
        String json = JsonUtils.toJson(event);
        assertEquals(8, JsonUtils.readTree(json).size());
        assertEquals("USD", JsonUtils.readTree(json).path("grossAmount").path("currency").asText());
        assertEquals(event, JsonUtils.fromJson(json, SettlementCompletedEvent.class));
    }
}
