package io.asteria.settlement.application.publisher;

import io.asteria.common.constants.SettlementTopics;
import io.asteria.settlement.application.service.SettlementOutboxTransactionService;
import io.asteria.settlement.domain.enums.SettlementOutboxEventType;
import io.asteria.settlement.domain.error.SettlementErrorCode;
import io.asteria.settlement.domain.exception.SettlementDomainException;
import io.asteria.settlement.domain.valueobject.SettlementOutboxEvent;
import io.asteria.common.trace.TraceConstants;
import io.asteria.common.trace.TraceIdGenerator;
import io.asteria.common.trace.TraceScope;
import org.slf4j.MDC;
import org.apache.kafka.clients.producer.ProducerRecord;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

/**
 * Settlement 消息发布器。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SettlementMessagePublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final SettlementOutboxTransactionService settlementOutboxTransactionService;

    /**
     * 发布 Outbox 事件
     */
    public void publish(SettlementOutboxEvent event) {
        try (TraceScope scope = TraceScope.open(TraceIdGenerator.resolve(event.getTraceId()))) {
            if (event.getEventType() == SettlementOutboxEventType.SETTLEMENT_COMPLETED) {
                publishSettlementCompleted(event);
                return;
            }

            log.error("Unsupported outbox event type, eventId: {}, settlementBatchId: {}, eventType: {}",
                    event.getEventId(), event.getAggregateId(), event.getEventType());
            throw new SettlementDomainException(SettlementErrorCode.UNSUPPORTED_OUTBOX_EVENT_TYPE);
        }
    }

    /**
     * 发布结算完成事件
     */
    private void publishSettlementCompleted(SettlementOutboxEvent event) {

        String traceId = MDC.get(TraceConstants.TRACE_ID);
        ProducerRecord<String, String> record = new ProducerRecord<>(
                SettlementTopics.SETTLEMENT_COMPLETED,
                event.getAggregateId(),
                event.getPayload()
        );

        record.headers().add(TraceConstants.TRACE_ID_HEADER, traceId.getBytes(StandardCharsets.UTF_8));
        CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(record);
        future.whenComplete((result, exception) -> {
            try (TraceScope scope = TraceScope.open(traceId)) {
                if (exception != null) {
                    log.error("Failed to publish outbox event, eventId: {}, settlementBatchId: {}, eventType: {}",
                            event.getEventId(), event.getAggregateId(), event.getEventType(), exception);
                    return;
                }

                try {
                    settlementOutboxTransactionService.markPublished(event.getId(), Instant.now());
                    log.info("Outbox event marked published, eventId: {}, settlementBatchId: {}, eventType: {}",
                            event.getEventId(), event.getAggregateId(), event.getEventType());
                } catch (Exception ex) {
                    log.error("Failed to mark outbox event published, eventId: {}, settlementBatchId: {}, eventType: {}",
                            event.getEventId(), event.getAggregateId(), event.getEventType(), ex);
                }
            }
        });
    }
}
