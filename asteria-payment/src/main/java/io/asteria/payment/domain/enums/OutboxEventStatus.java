package io.asteria.payment.domain.enums;

/**
 * Outbox 事件状态
 */
public enum OutboxEventStatus {

    /** 初始状态，但未发送 */
    PENDING,

    /** 最终状态，已发送 */
    PUBLISHED
}