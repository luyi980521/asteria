package io.asteria.payment.application.assembler;

import io.asteria.common.application.port.DistributedIdGenerator;
import io.asteria.common.util.JsonUtils;
import io.asteria.payment.domain.entity.Payment;
import io.asteria.payment.domain.enums.OutboxEventStatus;
import io.asteria.payment.domain.enums.OutboxEventType;
import io.asteria.payment.domain.event.PaymentCapturedEvent;
import io.asteria.payment.domain.valueobject.OutboxEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Payment Outbox 事件组装器
 */
@Component
@RequiredArgsConstructor
public class PaymentOutboxEventAssembler {

    private final DistributedIdGenerator distributedIdGenerator;

    /**
     * 组装支付捕获成功 Outbox 事件
     */
    public OutboxEvent toPaymentCapturedOutboxEvent(Payment payment) {

        PaymentCapturedEvent event = PaymentCapturedEvent.builder()
                .eventId(String.valueOf(distributedIdGenerator.nextId()))
                .paymentId(payment.getPaymentId())
                .paymentReference(payment.getReference())
                .amount(payment.getAmount())
                .capturedAt(payment.getCapturedAt())
                .build();

        return OutboxEvent.builder()
                .id(distributedIdGenerator.nextId())
                .eventId(event.eventId())
                .aggregateType("PAYMENT")
                .aggregateId(String.valueOf(payment.getPaymentId()))
                .eventType(OutboxEventType.PAYMENT_CAPTURED)
                .payload(JsonUtils.toJson(event))
                .status(OutboxEventStatus.PENDING)
                .createdAt(Instant.now())
                .build();
    }
}