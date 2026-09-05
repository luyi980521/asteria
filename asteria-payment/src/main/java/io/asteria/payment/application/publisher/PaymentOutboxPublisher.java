package io.asteria.payment.application.publisher;

import io.asteria.payment.domain.repository.PaymentOutboxEventRepository;
import io.asteria.payment.domain.valueobject.OutboxEvent;
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
public class PaymentOutboxPublisher {

    private final PaymentOutboxEventRepository paymentOutboxEventRepository;
    private final PaymentMessagePublisher paymentMessagePublisher;

    /**
     * 发布待发送 Outbox 事件
     */
    public void publishPendingEvents() {
        List<OutboxEvent> events = paymentOutboxEventRepository.findPending(100);

        for (OutboxEvent event : events) {
            publish(event);
        }
    }

    private void publish(OutboxEvent event) {

        try {
            paymentMessagePublisher.publish(event);
        } catch (Exception ex) {
            log.error("Failed to submit outbox event, eventId: {}, eventType: {}",
                    event.getEventId(), event.getEventType(), ex);
        }
    }
}
