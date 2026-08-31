package io.asteria.settlement.application.port.router;

import io.asteria.settlement.application.port.channel.SettlementChannel;
import io.asteria.settlement.domain.entity.SettlementBatch;
import io.asteria.settlement.domain.error.SettlementErrorCode;
import io.asteria.settlement.domain.exception.SettlementDomainException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Settlement Channel 路由器。
 */
@Component
@RequiredArgsConstructor
public class SettlementChannelRouter {

    private final List<SettlementChannel> settlementChannels;

    /**
     * 路由结算渠道。
     */
    public SettlementChannel route(SettlementBatch settlementBatch) {
        return settlementChannels.stream()
                .filter(channel -> channel.supports(settlementBatch))
                .findFirst()
                .orElseThrow(() -> new SettlementDomainException(
                        SettlementErrorCode.SETTLEMENT_CHANNEL_NOT_FOUND));
    }
}