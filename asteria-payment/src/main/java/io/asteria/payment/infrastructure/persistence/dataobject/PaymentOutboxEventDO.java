package io.asteria.payment.infrastructure.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.Instant;

/**
 * 支付模块本地消息表，用于 Transactional Outbox，保证支付状态变更与待发送事件在同一本地事务中持久化
 * @TableName payment_outbox_event
 */
@TableName(value ="payment_outbox_event")
@Data
public class PaymentOutboxEventDO {
    /**
     * 本地消息记录主键，使用雪花 ID
     */
    @TableId
    private Long id;

    /**
     * 业务事件唯一 ID，用于消息幂等和跨系统追踪
     */
    private String eventId;

    /**
     * 聚合类型，例如 PAYMENT
     */
    private String aggregateType;

    /**
     * 聚合 ID，例如 PaymentId
     */
    private String aggregateId;

    /**
     * 事件类型，例如 PAYMENT_CAPTURED
     */
    private String eventType;

    /**
     * 待发送消息的 JSON 内容
     */
    private String payload;

    /**
     * 消息发布状态：PENDING-待发布，PUBLISHED-已发布
     */
    private String status;

    /**
     * 本地消息创建时间
     */
    private Instant createdAt;

    /**
     * 消息成功发布到 MQ 的时间
     */
    private Instant publishedAt;
}