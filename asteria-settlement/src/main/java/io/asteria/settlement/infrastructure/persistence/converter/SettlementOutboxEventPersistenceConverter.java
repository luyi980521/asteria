package io.asteria.settlement.infrastructure.persistence.converter;

import io.asteria.settlement.domain.enums.SettlmementOutboxEventStatus;
import io.asteria.settlement.domain.enums.SettlementOutboxEventType;
import io.asteria.settlement.domain.valueobject.SettlementOutboxEvent;
import io.asteria.settlement.infrastructure.persistence.dataobject.SettlementOutboxEventDO;
import org.springframework.stereotype.Component;

/**
 * Settlement Outbox 事件持久化转换器
 */
@Component
public class SettlementOutboxEventPersistenceConverter {

    /**
     * 转换为持久化对象
     */
    public SettlementOutboxEventDO toDO(SettlementOutboxEvent event) {
        SettlementOutboxEventDO eventDO = new SettlementOutboxEventDO();

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
    public SettlementOutboxEvent toDomain(SettlementOutboxEventDO eventDO) {
        return new SettlementOutboxEvent(
                eventDO.getId(),
                eventDO.getEventId(),
                eventDO.getAggregateType(),
                eventDO.getAggregateId(),
                SettlementOutboxEventType.valueOf(eventDO.getEventType()),
                eventDO.getPayload(),
                SettlmementOutboxEventStatus.valueOf(eventDO.getStatus()),
                eventDO.getCreatedAt(),
                eventDO.getPublishedAt()
        );
    }
}