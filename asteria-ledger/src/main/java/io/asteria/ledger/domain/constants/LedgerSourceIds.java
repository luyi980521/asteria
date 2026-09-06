package io.asteria.ledger.domain.constants;

/**
 * Ledger SourceId 工具
 */
public final class LedgerSourceIds {

    private static final String PAYMENT_PREFIX = "PAYMENT_";
    private static final String SETTLEMENT_PREFIX = "SETTLEMENT_";

    /**
     * 生成支付 SourceId
     */
    public static String payment(Long paymentId) {
        return PAYMENT_PREFIX + paymentId;
    }

    /**
     * 生成结算批次 SourceId
     */
    public static String settlementBatch(Long settlementBatchId) {
        return SETTLEMENT_PREFIX + settlementBatchId;
    }
}