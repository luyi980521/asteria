CREATE TABLE balance (
    id                 BIGINT PRIMARY KEY,
    balance_account_id BIGINT NOT NULL,
    currency           VARCHAR(16) NOT NULL,
    available_amount   NUMERIC(38, 18) NOT NULL DEFAULT 0,
    reserved_amount    NUMERIC(38, 18) NOT NULL DEFAULT 0,
    version            BIGINT NOT NULL DEFAULT 0,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_balance_id CHECK (id > 0),
    CONSTRAINT ck_balance_account_id CHECK (balance_account_id > 0),
    CONSTRAINT ck_balance_currency CHECK (currency ~ '^[A-Z]{3}$'),
    CONSTRAINT ck_balance_available_amount CHECK (available_amount >= 0),
    CONSTRAINT ck_balance_reserved_amount CHECK (reserved_amount >= 0),
    CONSTRAINT ck_balance_version CHECK (version >= 0),
    CONSTRAINT uk_balance_account_currency UNIQUE (balance_account_id, currency)
);

COMMENT ON TABLE balance IS '按余额账户及币种维护可用金额和预留金额';
COMMENT ON COLUMN balance.id IS '余额ID，由应用生成';
COMMENT ON COLUMN balance.balance_account_id IS '余额账户ID，当前不映射 Ledger 账户';
COMMENT ON COLUMN balance.currency IS '币种，可用金额与预留金额共用';
COMMENT ON COLUMN balance.available_amount IS '可用余额，允许零，不允许负数';
COMMENT ON COLUMN balance.reserved_amount IS '预留余额，允许零，不允许负数';
COMMENT ON COLUMN balance.version IS '乐观锁版本，条件更新时由数据库检查并递增';
COMMENT ON COLUMN balance.created_at IS '创建时间';
COMMENT ON COLUMN balance.updated_at IS '最后更新时间，由应用更新';
