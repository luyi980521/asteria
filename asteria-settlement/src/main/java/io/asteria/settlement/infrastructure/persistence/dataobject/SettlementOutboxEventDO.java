package io.asteria.settlement.infrastructure.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.Instant;

/**
 * 结算模块本地消息表，用于 Transactional Outbox，保证结算状态与结算事件原子持久化
 * @TableName settlement_outbox_event
 */
@TableName(value ="settlement_outbox_event")
@Data
public class SettlementOutboxEventDO {
    /**
     * 本地消息记录 ID，使用雪花 ID
     */
    @TableId
    private Long id;

    /**
     * 业务事件唯一 ID
     */
    private String eventId;

    /**
     * 聚合类型，例如 SETTLEMENT_BATCH
     */
    private String aggregateType;

    /**
     * 聚合 ID，例如 SettlementBatchId
     */
    private String aggregateId;

    /**
     * 事件类型，例如 SETTLEMENT_COMPLETED
     */
    private String eventType;

    /**
     * 待发送消息 JSON 内容
     */
    private String payload;

    /** Persisted correlation ID; nullable for historical events. */
    private String traceId;

    /**
     * 发布状态：PENDING-待发布，PUBLISHED-已发布
     */
    private String status;

    /**
     * 事件创建时间
     */
    private Instant createdAt;

    /**
     * 成功发布到 MQ 的时间
     */
    private Instant publishedAt;
}
