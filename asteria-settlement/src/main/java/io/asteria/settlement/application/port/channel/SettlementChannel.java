package io.asteria.settlement.application.port.channel;

import io.asteria.settlement.domain.entity.SettlementBatch;

/**
 * Settlement Channel。
 */
public interface SettlementChannel {

    /**
     * 是否支持当前结算批次。
     */
    boolean supports(SettlementBatch settlementBatch);

    /**
     * 提交结算批次。
     */
    SettlementBatchResult submit(SettlementBatchRequest request);
}