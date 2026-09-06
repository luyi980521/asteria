CREATE TABLE settlement_outbox_event (
    id BIGINT PRIMARY KEY,
    event_id VARCHAR(64) NOT NULL,
    aggregate_type VARCHAR(64) NOT NULL,
    aggregate_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(128) NOT NULL,
    payload JSONB NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    published_at TIMESTAMPTZ,
    trace_id VARCHAR(64),

    CONSTRAINT uk_settlement_outbox_event_id UNIQUE (event_id),

    CONSTRAINT ck_settlement_outbox_event_status
        CHECK (status IN ('PENDING', 'PUBLISHED'))
);

COMMENT ON TABLE settlement_outbox_event
    IS '结算模块本地消息表，用于 Transactional Outbox，保证结算状态与结算事件原子持久化';

COMMENT ON COLUMN settlement_outbox_event.id
    IS '本地消息记录 ID，使用雪花 ID';

COMMENT ON COLUMN settlement_outbox_event.event_id
    IS '业务事件唯一 ID';

COMMENT ON COLUMN settlement_outbox_event.aggregate_type
    IS '聚合类型，例如 SETTLEMENT_BATCH';

COMMENT ON COLUMN settlement_outbox_event.aggregate_id
    IS '聚合 ID，例如 SettlementBatchId';

COMMENT ON COLUMN settlement_outbox_event.event_type
    IS '事件类型，例如 SETTLEMENT_COMPLETED';

COMMENT ON COLUMN settlement_outbox_event.payload
    IS '待发送消息 JSON 内容';

COMMENT ON COLUMN settlement_outbox_event.status
    IS '发布状态：PENDING-待发布，PUBLISHED-已发布';

COMMENT ON COLUMN settlement_outbox_event.created_at
    IS '事件创建时间';

COMMENT ON COLUMN settlement_outbox_event.published_at
    IS '成功发布到 MQ 的时间';

comment on column settlement_outbox_event.trace_id is '链路追踪id';

CREATE INDEX idx_settlement_outbox_event_pending
    ON settlement_outbox_event (status, created_at);
