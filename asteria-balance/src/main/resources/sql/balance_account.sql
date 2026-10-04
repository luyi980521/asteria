CREATE TABLE balance_account (
    id                 BIGINT PRIMARY KEY,
    owner_type         VARCHAR(32) NOT NULL,
    owner_id           BIGINT NOT NULL,
    status             VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at         TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_balance_account_id CHECK (id > 0),
    CONSTRAINT ck_balance_account_owner_type CHECK (owner_type IN ('MERCHANT', 'CUSTOMER', 'PLATFORM')),
    CONSTRAINT ck_balance_account_owner_id CHECK (owner_id > 0),
    CONSTRAINT ck_balance_account_status CHECK (status IN ('ACTIVE', 'FROZEN', 'CLOSED')),
    CONSTRAINT uk_balance_account_owner UNIQUE (owner_type, owner_id)
);

COMMENT ON TABLE balance_account IS '按所属主体唯一的余额账户，具体币种及金额由余额表维护';
COMMENT ON COLUMN balance_account.id IS '余额账户ID，由应用生成';
COMMENT ON COLUMN balance_account.owner_type IS '所属主体类型：MERCHANT、CUSTOMER、PLATFORM';
COMMENT ON COLUMN balance_account.owner_id IS '所属主体ID';
COMMENT ON COLUMN balance_account.status IS '账户状态：ACTIVE、FROZEN、CLOSED';
COMMENT ON COLUMN balance_account.created_at IS '创建时间';
COMMENT ON COLUMN balance_account.updated_at IS '最后更新时间，由应用更新';
