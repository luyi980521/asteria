package io.asteria.payment.application.publisher;

import io.asteria.common.constants.PaymentTopics;
import io.asteria.payment.application.service.PaymentOutboxTransactionService;
import io.asteria.payment.domain.enums.OutboxEventType;
import io.asteria.payment.domain.error.PaymentErrorCode;
import io.asteria.payment.domain.exception.PaymentDomainException;
import io.asteria.payment.domain.valueobject.OutboxEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

/**
 * Payment 消息发布器。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentMessagePublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final PaymentOutboxTransactionService paymentOutboxTransactionService;

    /**
     * 发布 Outbox 事件
     */
    public void publish(OutboxEvent event) {
        if (event.getEventType() == OutboxEventType.PAYMENT_CAPTURED) {
            publishPaymentCaptured(event);
            return;
        }

        log.error("Unsupported outbox event type: {}", event.getEventType());
        throw new PaymentDomainException(PaymentErrorCode.UNSUPPORTED_OUTBOX_EVENT_TYPE);
    }

    /**
     * 发布支付捕获成功事件
     */
    private void publishPaymentCaptured(OutboxEvent event) {

        CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(
                PaymentTopics.PAYMENT_CAPTURED,
                event.getAggregateId(),
                event.getPayload()
        );

        future.whenComplete((result, exception) -> {
            if (exception != null) {
                log.error("Failed to publish outbox event, eventId: {}, eventType: {}",
                        event.getEventId(), event.getEventType(), exception);
                return;
            }

            try {
                paymentOutboxTransactionService.markPublished(event.getId(), Instant.now());
                log.info("Outbox event marked published, eventId: {}, eventType: {}",
                        event.getEventId(), event.getEventType());
            } catch (Exception ex) {
                log.error("Failed to mark outbox event published, eventId: {}",
                        event.getEventId(), ex);
            }
        });
    }
}
