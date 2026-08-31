CREATE TABLE settlement_batch (
    id BIGINT PRIMARY KEY,
    settlement_reference VARCHAR(64) NOT NULL,
    currency VARCHAR(16) NOT NULL,
    gross_amount NUMERIC(24, 8) NOT NULL,
    fee_amount NUMERIC(24, 8) NOT NULL,
    net_amount NUMERIC(24, 8) NOT NULL,
    status VARCHAR(32) NOT NULL,
    channel_settlement_batch_id VARCHAR(128),
    created_at TIMESTAMPTZ NOT NULL,
    processing_at TIMESTAMPTZ,
    accepted_at TIMESTAMPTZ,
    settled_at TIMESTAMPTZ,
    failed_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT uk_settlement_batch_reference UNIQUE (settlement_reference),
    CONSTRAINT ck_settlement_batch_gross_amount CHECK (gross_amount > 0),
    CONSTRAINT ck_settlement_batch_fee_amount CHECK (fee_amount >= 0),
    CONSTRAINT ck_settlement_batch_net_amount CHECK (net_amount >= 0),
    CONSTRAINT ck_settlement_batch_status CHECK (
        status IN ('CREATED', 'PROCESSING', 'ACCEPTED', 'SETTLED', 'FAILED')
    )
);

COMMENT ON TABLE settlement_batch IS '结算批次表，记录一组待结算资金的批次级状态与金额汇总';

COMMENT ON COLUMN settlement_batch.id IS '结算批次 ID，使用雪花 ID';
COMMENT ON COLUMN settlement_batch.settlement_reference IS '结算批次业务唯一引用';
COMMENT ON COLUMN settlement_batch.currency IS '结算币种';
COMMENT ON COLUMN settlement_batch.gross_amount IS '批次结算总金额，未扣除手续费';
COMMENT ON COLUMN settlement_batch.fee_amount IS '批次手续费金额';
COMMENT ON COLUMN settlement_batch.net_amount IS '批次净结算金额，通常等于 gross_amount - fee_amount';
COMMENT ON COLUMN settlement_batch.status IS '结算批次状态：CREATED-已创建，PROCESSING-处理中，ACCEPTED-渠道已受理，SETTLED-已结算，FAILED-失败';
COMMENT ON COLUMN settlement_batch.channel_settlement_batch_id IS '上游渠道返回的结算批次 ID';
COMMENT ON COLUMN settlement_batch.created_at IS '结算批次创建时间';
COMMENT ON COLUMN settlement_batch.processing_at IS '开始处理结算批次的时间';
COMMENT ON COLUMN settlement_batch.accepted_at IS '上游渠道受理结算批次的时间';
COMMENT ON COLUMN settlement_batch.settled_at IS '结算资金实际完成的时间';
COMMENT ON COLUMN settlement_batch.failed_at IS '结算批次失败时间';
COMMENT ON COLUMN settlement_batch.updated_at IS '最后更新时间';