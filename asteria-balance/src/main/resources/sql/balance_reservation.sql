CREATE TABLE balance_reservation (
    id             BIGINT PRIMARY KEY,
    balance_id     BIGINT NOT NULL,
    amount         NUMERIC(38, 18) NOT NULL,
    currency       VARCHAR(16) NOT NULL,
    status         VARCHAR(32) NOT NULL,
    reference_type VARCHAR(64) NOT NULL,
    reference_id   VARCHAR(128) NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_balance_reservation_id CHECK (id > 0),
    CONSTRAINT ck_balance_reservation_balance_id CHECK (balance_id > 0),
    CONSTRAINT ck_balance_reservation_amount CHECK (amount > 0),
    CONSTRAINT ck_balance_reservation_currency CHECK (currency ~ '^[A-Z]{3}$'),
    CONSTRAINT ck_balance_reservation_status CHECK (status IN ('RESERVED', 'RELEASED', 'CONSUMED')),
    CONSTRAINT ck_balance_reservation_reference_type CHECK (length(trim(reference_type)) > 0),
    CONSTRAINT ck_balance_reservation_reference_id CHECK (length(trim(reference_id)) > 0),
    CONSTRAINT uk_balance_reservation_reference UNIQUE (balance_id, reference_type, reference_id)
);

CREATE INDEX idx_balance_reservation_balance_status ON balance_reservation (balance_id, status);

COMMENT ON TABLE balance_reservation IS '余额预留记录，生效中的预留可释放或消费';
COMMENT ON COLUMN balance_reservation.id IS '余额预留ID，由应用生成';
COMMENT ON COLUMN balance_reservation.balance_id IS '所属余额ID，不建立数据库外键';
COMMENT ON COLUMN balance_reservation.amount IS '预留金额，必须大于零';
COMMENT ON COLUMN balance_reservation.currency IS '预留金额币种，后续写入时校验与余额一致';
COMMENT ON COLUMN balance_reservation.status IS '预留状态：RESERVED、RELEASED、CONSUMED';
COMMENT ON COLUMN balance_reservation.reference_type IS '业务引用类型';
COMMENT ON COLUMN balance_reservation.reference_id IS '业务引用ID';
COMMENT ON COLUMN balance_reservation.created_at IS '创建时间';
COMMENT ON COLUMN balance_reservation.updated_at IS '最后更新时间，由应用更新';
