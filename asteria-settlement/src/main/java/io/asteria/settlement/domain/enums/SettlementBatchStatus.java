package io.asteria.settlement.domain.enums;

/**
 * 结算批次状态
 */
public enum SettlementBatchStatus {

    /** 已创建 */
    CREATED,

    /** 处理中 */
    PROCESSING,

    /** 渠道已受理 */
    ACCEPTED,

    /** 提交结果未知 */
    SUBMIT_UNKNOWN,

    /** 已结算 */
    SETTLED,

    /** 失败 */
    FAILED
}