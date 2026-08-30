package io.asteria.payment.application.publisher;

import io.asteria.payment.domain.repository.OutboxEventRepository;
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
public class OutboxPublisher {

    private final OutboxEventRepository outboxEventRepository;

    /**
     * 发布待发送 Outbox 事件
     */
    public void publishPendingEvents() {
        List<OutboxEvent> events = outboxEventRepository.findPending(100);

        for (OutboxEvent event : events) {
            publish(event);
        }
    }

    private void publish(OutboxEvent event) {
        // TODO 下一步这里接 MQ producer
    }
}