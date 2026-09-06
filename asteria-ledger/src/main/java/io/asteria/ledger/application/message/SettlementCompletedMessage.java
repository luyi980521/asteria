package io.asteria.ledger.application.message;

import io.asteria.common.domain.valueobject.Money;

import java.time.Instant;

/**
 * 结算完成事件 payload
 * */
public record SettlementCompletedMessage(
        /* 事件 ID */
        String eventId,

        /* 结算批次 ID */
        Long settlementBatchId,

        /* 结算批次业务引用 */
        String settlementReference,

        /* 渠道结算批次 ID */
        String channelSettlementBatchId,

        /* 结算总金额 */
        Money grossAmount,

        /* 结算手续费 */
        Money feeAmount,

        /* 实际到账金额 */
        Money netAmount,

        /* 结算完成时间 */
        Instant settledAt
) {
}
