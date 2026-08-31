CREATE TABLE settlement_item (
    id BIGINT PRIMARY KEY,
    settlement_batch_id BIGINT NOT NULL,
    payment_id BIGINT NOT NULL,
    payment_reference VARCHAR(64) NOT NULL,
    amount NUMERIC(24, 8) NOT NULL,
    currency VARCHAR(16) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT ck_settlement_item_amount CHECK (amount > 0)
);

CREATE INDEX idx_settlement_batch_status
    ON settlement_batch (status);

CREATE INDEX idx_settlement_item_batch_id
    ON settlement_item (settlement_batch_id);

CREATE INDEX idx_settlement_item_payment_id
    ON settlement_item (payment_id);

COMMENT ON TABLE settlement_item IS '结算批次明细表，记录结算批次包含的具体支付资金明细';

COMMENT ON COLUMN settlement_item.id IS '结算明细 ID，使用雪花 ID';
COMMENT ON COLUMN settlement_item.settlement_batch_id IS '所属结算批次 ID';
COMMENT ON COLUMN settlement_item.payment_id IS '关联的 Payment ID';
COMMENT ON COLUMN settlement_item.payment_reference IS '关联的 Payment 业务引用';
COMMENT ON COLUMN settlement_item.amount IS '该支付明细参与结算的金额';
COMMENT ON COLUMN settlement_item.currency IS '该支付明细的币种';
COMMENT ON COLUMN settlement_item.created_at IS '结算明细创建时间';