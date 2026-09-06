package io.asteria.settlement.application.assembler;

import io.asteria.common.application.port.DistributedIdGenerator;
import io.asteria.common.util.JsonUtils;
import io.asteria.common.trace.TraceConstants;
import org.slf4j.MDC;
import io.asteria.settlement.domain.entity.SettlementBatch;
import io.asteria.settlement.domain.enums.SettlementOutboxEventType;
import io.asteria.settlement.domain.enums.SettlementOutboxEventStatus;
import io.asteria.settlement.domain.event.SettlementCompletedEvent;
import io.asteria.settlement.domain.valueobject.SettlementOutboxEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Settlement Outbox 事件组装器
 */
@Component
@RequiredArgsConstructor
public class SettlementOutboxEventAssembler {

    private final DistributedIdGenerator distributedIdGenerator;

    /**
     * 组装结算完成 Outbox 事件
     */
    public SettlementOutboxEvent toSettlementCompletedOutboxEvent(SettlementBatch settlementBatch) {

        SettlementCompletedEvent event = SettlementCompletedEvent.builder()
                .eventId(String.valueOf(distributedIdGenerator.nextId()))
                .settlementBatchId(settlementBatch.getSettlementBatchId().value())
                .settlementReference(settlementBatch.getReference().value())
                .channelSettlementBatchId(settlementBatch.getChannelSettlementBatchId())
                .grossAmount(settlementBatch.getGrossAmount())
                .feeAmount(settlementBatch.getFeeAmount())
                .netAmount(settlementBatch.getNetAmount())
                .settledAt(settlementBatch.getSettledAt())
                .build();

        return SettlementOutboxEvent.builder()
                .id(distributedIdGenerator.nextId())
                .eventId(event.eventId())
                .aggregateType("SETTLEMENT_BATCH")
                .aggregateId(String.valueOf(settlementBatch.getSettlementBatchId().value()))
                .eventType(SettlementOutboxEventType.SETTLEMENT_COMPLETED)
                .payload(JsonUtils.toJson(event))
                .traceId(MDC.get(TraceConstants.TRACE_ID))
                .status(SettlementOutboxEventStatus.PENDING)
                .createdAt(Instant.now())
                .build();
    }
}
