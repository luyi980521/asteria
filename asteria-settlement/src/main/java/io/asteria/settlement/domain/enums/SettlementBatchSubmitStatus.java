package io.asteria.settlement.domain.enums;

/**
 * 结算批次提交结果状态。
 */
public enum SettlementBatchSubmitStatus {

    /** 渠道已受理 */
    ACCEPTED,

    /** 渠道明确拒绝 */
    REJECTED,

    /** 提交结果未知 */
    UNKNOWN
}