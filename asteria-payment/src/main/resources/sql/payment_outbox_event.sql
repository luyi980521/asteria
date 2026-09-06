CREATE TABLE payment_outbox_event (
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

    CONSTRAINT uk_payment_outbox_event_event_id UNIQUE (event_id),
    CONSTRAINT ck_payment_outbox_event_status
        CHECK (status IN ('PENDING', 'PUBLISHED'))
);

CREATE INDEX idx_payment_outbox_event_pending
    ON payment_outbox_event (status, created_at);

COMMENT ON TABLE payment_outbox_event IS '支付模块本地消息表，用于 Transactional Outbox，保证支付状态变更与待发送事件在同一本地事务中持久化';

COMMENT ON COLUMN payment_outbox_event.id IS '本地消息记录主键，使用雪花 ID';
COMMENT ON COLUMN payment_outbox_event.event_id IS '业务事件唯一 ID，用于消息幂等和跨系统追踪';
COMMENT ON COLUMN payment_outbox_event.aggregate_type IS '聚合类型，例如 PAYMENT';
COMMENT ON COLUMN payment_outbox_event.aggregate_id IS '聚合 ID，例如 PaymentId';
COMMENT ON COLUMN payment_outbox_event.event_type IS '事件类型，例如 PAYMENT_CAPTURED';
COMMENT ON COLUMN payment_outbox_event.payload IS '待发送消息的 JSON 内容';
COMMENT ON COLUMN payment_outbox_event.status IS '消息发布状态：PENDING-待发布，PUBLISHED-已发布';
COMMENT ON COLUMN payment_outbox_event.created_at IS '本地消息创建时间';
COMMENT ON COLUMN payment_outbox_event.published_at IS '消息成功发布到 MQ 的时间';
comment on column payment_outbox_event.trace_id is '链路追踪id';