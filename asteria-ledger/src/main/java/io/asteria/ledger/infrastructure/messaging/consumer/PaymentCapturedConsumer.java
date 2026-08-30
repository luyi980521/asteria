package io.asteria.ledger.infrastructure.messaging.consumer;

import io.asteria.common.util.JsonUtils;
import io.asteria.ledger.application.message.PaymentCapturedMessage;
import io.asteria.ledger.application.service.PaymentCapturedLedgerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * 支付捕获事件消费者
 * */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentCapturedConsumer {

    private final PaymentCapturedLedgerService paymentCapturedLedgerService;

    /**
     * 消费支付捕获成功消息。
     */
    @KafkaListener(
            topics = "payment-captured",
            groupId = "asteria-ledger-payment-captured"
    )
    public void consume(String payload) {
        PaymentCapturedMessage message = JsonUtils.fromJson(payload, PaymentCapturedMessage.class);

        log.info("Received payment captured event, eventId: {}, paymentId: {}",
                message.eventId(), message.paymentId());

        paymentCapturedLedgerService.handle(message);
    }
}
