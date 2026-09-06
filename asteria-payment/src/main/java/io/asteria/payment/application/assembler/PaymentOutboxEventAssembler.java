package io.asteria.payment.application.assembler;

import io.asteria.common.application.port.DistributedIdGenerator;
import io.asteria.common.util.JsonUtils;
import io.asteria.common.trace.TraceConstants;
import org.slf4j.MDC;
import io.asteria.payment.domain.entity.Payment;
import io.asteria.payment.domain.enums.PaymentOutboxEventStatus;
import io.asteria.payment.domain.enums.PaymentOutboxEventType;
import io.asteria.payment.domain.event.PaymentCapturedEvent;
import io.asteria.payment.domain.valueobject.PaymentOutboxEvent;
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
    public PaymentOutboxEvent toPaymentCapturedOutboxEvent(Payment payment) {

        PaymentCapturedEvent event = PaymentCapturedEvent.builder()
                .eventId(String.valueOf(distributedIdGenerator.nextId()))
                .paymentId(payment.getPaymentId().value())
                .paymentReference(PaymentCapturedEvent.Reference.builder()
                        .referenceType(payment.getReference().referenceType())
                        .referenceId(payment.getReference().referenceId())
                        .build())
                .amount(payment.getAmount())
                .capturedAt(payment.getCapturedAt())
                .build();

        return PaymentOutboxEvent.builder()
                .id(distributedIdGenerator.nextId())
                .eventId(event.eventId())
                .aggregateType("PAYMENT")
                .aggregateId(String.valueOf(payment.getPaymentId()))
                .eventType(PaymentOutboxEventType.PAYMENT_CAPTURED)
                .payload(JsonUtils.toJson(event))
                .traceId(MDC.get(TraceConstants.TRACE_ID))
                .status(PaymentOutboxEventStatus.PENDING)
                .createdAt(Instant.now())
                .build();
    }
}
