package io.asteria.settlement.infrastructure.persistence.channel;

import io.asteria.common.application.port.DistributedIdGenerator;
import io.asteria.settlement.application.port.channel.SettlementBatchRequest;
import io.asteria.settlement.application.port.channel.SettlementBatchResult;
import io.asteria.settlement.application.port.channel.SettlementChannel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Mock Settlement Channel。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MockSettlementChannel implements SettlementChannel {

    private final DistributedIdGenerator distributedIdGenerator;

    /**
     * 模拟提交结算批次。
     */
    @Override
    public SettlementBatchResult submit(SettlementBatchRequest request) {
        String channelSettlementBatchId =
                "SETTLEMENT-BATCH-" + distributedIdGenerator.nextId();

        log.info("Mock settlement batch accepted, settlementBatchId: {}, channelSettlementBatchId: {}",
                request.settlementBatchId(), channelSettlementBatchId);

        return SettlementBatchResult.builder()
                .accepted(true)
                .channelSettlementBatchId(channelSettlementBatchId)
                .acceptedAt(Instant.now())
                .build();
    }
}