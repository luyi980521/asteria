package io.asteria.payment.infrastructure.persistence.converter;

import io.asteria.payment.domain.enums.PaymentOutboxEventStatus;
import io.asteria.payment.domain.enums.PaymentOutboxEventType;
import io.asteria.payment.domain.valueobject.PaymentOutboxEvent;
import io.asteria.payment.infrastructure.persistence.dataobject.PaymentOutboxEventDO;
import org.springframework.stereotype.Component;

/**
 * Payment Outbox 事件持久化转换器
 */
@Component
public class PaymentOutboxEventPersistenceConverter {

    /**
     * 转换为持久化对象
     */
    public PaymentOutboxEventDO toDO(PaymentOutboxEvent event) {
        PaymentOutboxEventDO eventDO = new PaymentOutboxEventDO();

        eventDO.setId(event.getId());
        eventDO.setEventId(event.getEventId());
        eventDO.setAggregateType(event.getAggregateType());
        eventDO.setAggregateId(event.getAggregateId());
        eventDO.setEventType(event.getEventType().name());
        eventDO.setPayload(event.getPayload());
        eventDO.setStatus(event.getStatus().name());
        eventDO.setCreatedAt(event.getCreatedAt());
        eventDO.setPublishedAt(event.getPublishedAt());

        return eventDO;
    }

    /**
     * 转换为领域对象
     */
    public PaymentOutboxEvent toDomain(PaymentOutboxEventDO eventDO) {
        return new PaymentOutboxEvent(
                eventDO.getId(),
                eventDO.getEventId(),
                eventDO.getAggregateType(),
                eventDO.getAggregateId(),
                PaymentOutboxEventType.valueOf(eventDO.getEventType()),
                eventDO.getPayload(),
                PaymentOutboxEventStatus.valueOf(eventDO.getStatus()),
                eventDO.getCreatedAt(),
                eventDO.getPublishedAt()
        );
    }
}