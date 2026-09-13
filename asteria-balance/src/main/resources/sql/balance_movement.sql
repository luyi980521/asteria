CREATE TABLE balance_movement (
    id             BIGINT PRIMARY KEY,
    balance_id     BIGINT NOT NULL,
    movement_type  VARCHAR(32) NOT NULL,
    amount         NUMERIC(38, 18) NOT NULL,
    currency       VARCHAR(16) NOT NULL,
    reference_type VARCHAR(64) NOT NULL,
    reference_id   VARCHAR(128) NOT NULL,
    event_id       VARCHAR(128) NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_balance_movement_id CHECK (id > 0),
    CONSTRAINT ck_balance_movement_balance_id CHECK (balance_id > 0),
    CONSTRAINT ck_balance_movement_type CHECK (
        movement_type IN ('CREDIT', 'RESERVE', 'RELEASE', 'DEBIT_RESERVED')
    ),
    CONSTRAINT ck_balance_movement_amount CHECK (amount > 0),
    CONSTRAINT ck_balance_movement_currency CHECK (currency ~ '^[A-Z]{3}$'),
    CONSTRAINT ck_balance_movement_reference_type CHECK (length(trim(reference_type)) > 0),
    CONSTRAINT ck_balance_movement_reference_id CHECK (length(trim(reference_id)) > 0),
    CONSTRAINT ck_balance_movement_event_id CHECK (length(trim(event_id)) > 0),
    CONSTRAINT uk_balance_movement_event UNIQUE (balance_id, movement_type, event_id)
);

CREATE INDEX idx_balance_movement_balance_created_at ON balance_movement (balance_id, created_at DESC);
CREATE INDEX idx_balance_movement_reference ON balance_movement (reference_type, reference_id);

COMMENT ON TABLE balance_movement IS '余额变化审计流水，金额为正，由变化类型决定语义';
COMMENT ON COLUMN balance_movement.id IS '余额流水ID，由应用生成';
COMMENT ON COLUMN balance_movement.balance_id IS '所属余额ID，不建立数据库外键';
COMMENT ON COLUMN balance_movement.movement_type IS '变化类型：CREDIT、RESERVE、RELEASE、DEBIT_RESERVED';
COMMENT ON COLUMN balance_movement.amount IS '变化金额，必须大于零';
COMMENT ON COLUMN balance_movement.currency IS '变化金额币种，后续写入时校验与余额一致';
COMMENT ON COLUMN balance_movement.reference_type IS '业务引用类型';
COMMENT ON COLUMN balance_movement.reference_id IS '业务引用ID';
COMMENT ON COLUMN balance_movement.event_id IS '幂等事件引用，唯一范围为余额及变化类型';
COMMENT ON COLUMN balance_movement.created_at IS '创建时间';
