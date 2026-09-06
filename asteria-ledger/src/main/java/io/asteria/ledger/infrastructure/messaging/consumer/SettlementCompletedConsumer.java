package io.asteria.ledger.infrastructure.messaging.consumer;

import io.asteria.common.util.JsonUtils;
import io.asteria.common.trace.TraceConstants;
import io.asteria.infrastructure.trace.KafkaTraceUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.MDC;
import io.asteria.ledger.application.message.SettlementCompletedMessage;
import io.asteria.ledger.application.service.SettlementCompletedLedgerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * 结算完成消息消费者。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SettlementCompletedConsumer {

    private final SettlementCompletedLedgerService settlementCompletedLedgerService;

    /**
     * 消费结算完成消息。
     */
    @KafkaListener(
            topics = "settlement-completed",
            groupId = "asteria-ledger-settlement-completed"
    )
    public void consume(ConsumerRecord<String, String> record) {
        try {
            MDC.put(TraceConstants.TRACE_ID, KafkaTraceUtils.resolveTraceId(record.headers()));
            String payload = record.value();
            SettlementCompletedMessage message = JsonUtils.fromJson(payload, SettlementCompletedMessage.class);
            log.info("Received settlement completed event, eventId: {}, settlementBatchId: {}",
                    message.eventId(), message.settlementBatchId());

            settlementCompletedLedgerService.handle(message);
        } finally {
            MDC.remove(TraceConstants.TRACE_ID);
        }
    }
}
