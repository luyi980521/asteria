package io.asteria.settlement.application.publisher;

import io.asteria.settlement.domain.repository.SettlementOutboxRepository;
import io.asteria.settlement.domain.valueobject.SettlementOutboxEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Outbox 事件发布器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SettlementOutboxPublisher {

    private final SettlementOutboxRepository settlementOutboxRepository;
    private final SettlementMessagePublisher settlementMessagePublisher;

    /**
     * 发布待发送 Outbox 事件
     */
    public void publishPendingEvents() {
        List<SettlementOutboxEvent> events = settlementOutboxRepository.findPending(100);

        for (SettlementOutboxEvent event : events) {
            publish(event);
        }
    }

    private void publish(SettlementOutboxEvent event) {

        try {
            settlementMessagePublisher.publish(event);
        } catch (Exception ex) {
            log.error("Failed to submit outbox event, eventId: {}, settlementBatchId: {}, eventType: {}",
                    event.getEventId(), event.getAggregateId(), event.getEventType(), ex);
        }
    }
}
