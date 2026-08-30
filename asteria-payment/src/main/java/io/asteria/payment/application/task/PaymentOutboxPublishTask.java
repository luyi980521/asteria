package io.asteria.payment.application.task;

import io.asteria.payment.application.publisher.OutboxPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Payment Outbox 发布定时任务
 * */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "asteria.payment.outbox",
        name = "publish-task-enabled",
        havingValue = "true"
)
public class PaymentOutboxPublishTask {

    private final OutboxPublisher outboxPublisher;

    /**
     * 扫描并发布待发送的 Outbox 事件
     * */
    @Scheduled(fixedDelayString = "${asteria.payment.outbox.publish-interval-ms:1000}")
    public void publishPendingEvents() {
        try {
            outboxPublisher.publishPendingEvents();
        } catch (Exception ex) {
            log.error("Failed to execute payment outbox publish task: {}", ex.getMessage(), ex);
        }
    }
}
