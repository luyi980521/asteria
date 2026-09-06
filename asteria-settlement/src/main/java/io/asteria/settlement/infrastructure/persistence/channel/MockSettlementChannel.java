package io.asteria.settlement.infrastructure.persistence.channel;

import io.asteria.common.domain.valueobject.CurrencyCode;
import io.asteria.common.application.port.DistributedIdGenerator;
import io.asteria.settlement.application.port.channel.SettlementBatchRequest;
import io.asteria.settlement.application.port.channel.SettlementBatchResult;
import io.asteria.settlement.application.port.channel.SettlementChannel;
import io.asteria.settlement.domain.entity.SettlementBatch;
import io.asteria.settlement.domain.enums.SettlementBatchSubmitStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Set;

/**
 * Mock Settlement Channel。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MockSettlementChannel implements SettlementChannel {

    private final DistributedIdGenerator distributedIdGenerator;
    private static final Set<CurrencyCode> SUPPORTED_CURRENCIES = Set.of(
            CurrencyCode.of("USD"),
            CurrencyCode.of("EUR"),
            CurrencyCode.of("GBP"),
            CurrencyCode.of("JPY")
    );

    /**
     * 是否支持当前结算批次。
     */
    @Override
    public boolean supports(SettlementBatch settlementBatch) {
        return settlementBatch != null && SUPPORTED_CURRENCIES.contains(
                settlementBatch.getGrossAmount().currency()
        );
    }

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
                .status(SettlementBatchSubmitStatus.ACCEPTED)
                .channelSettlementBatchId(channelSettlementBatchId)
                .acceptedAt(Instant.now())
                .build();
    }
}