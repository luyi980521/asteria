package io.asteria.settlement.domain.enums;

/**
 * 结算 Outbox 事件状态
 */
public enum SettlementOutboxEventStatus {

    /** 初始状态，但未发送 */
    PENDING,

    /** 最终状态，已发送 */
    PUBLISHED
}