CREATE TABLE ledger_account (
    id                      BIGINT PRIMARY KEY,
    account_code            VARCHAR(128) NOT NULL,
    owner_type              VARCHAR(32) NOT NULL,
    owner_id                BIGINT NOT NULL,
    category                VARCHAR(32) NOT NULL,
    currency                VARCHAR(16) NOT NULL,
    status                  VARCHAR(32) NOT NULL,
    allow_negative_balance  BOOLEAN NOT NULL DEFAULT FALSE,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version                 BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT ck_ledger_account_id_positive
        CHECK (id > 0),

    CONSTRAINT ck_ledger_account_owner_id_positive
        CHECK (owner_id > 0),

    CONSTRAINT uk_ledger_account_code
        UNIQUE (account_code),

    CONSTRAINT ck_ledger_account_category
        CHECK (
            category IN (
                'ASSET',
                'LIABILITY',
                'EQUITY',
                'REVENUE',
                'EXPENSE'
            )
        ),

    CONSTRAINT ck_ledger_account_status
        CHECK (
            status IN (
                'ACTIVE',
                'FROZEN',
                'CLOSED'
            )
        ),

    CONSTRAINT ck_ledger_account_owner_type
        CHECK (
            owner_type IN (
                'USER',
                'MERCHANT',
                'PLATFORM',
                'CHANNEL',
                'BANK',
                'SYSTEM'
            )
        ),

    CONSTRAINT ck_ledger_account_currency_not_blank
        CHECK (length(trim(currency)) > 0),

    CONSTRAINT ck_ledger_account_version_non_negative
        CHECK (version >= 0)
);

COMMENT ON TABLE ledger_account
    IS '账本账户表';

COMMENT ON COLUMN ledger_account.id
    IS '账本账户ID，由应用层雪花算法生成';

COMMENT ON COLUMN ledger_account.account_code
    IS '账户业务编码，全局唯一';

COMMENT ON COLUMN ledger_account.owner_type
    IS '账户所属主体类型：USER、MERCHANT、PLATFORM、CHANNEL、BANK、SYSTEM';

COMMENT ON COLUMN ledger_account.owner_id
    IS '账户所属主体ID';

COMMENT ON COLUMN ledger_account.category
    IS '会计分类：ASSET、LIABILITY、EQUITY、REVENUE、EXPENSE';

COMMENT ON COLUMN ledger_account.currency
    IS '账户币种，例如 CNY、USD、EUR';

COMMENT ON COLUMN ledger_account.status
    IS '账户状态：ACTIVE、FROZEN、CLOSED';

COMMENT ON COLUMN ledger_account.allow_negative_balance
    IS '是否允许出现负余额';

COMMENT ON COLUMN ledger_account.created_at
    IS '数据库记录创建时间';

COMMENT ON COLUMN ledger_account.updated_at
    IS '数据库记录最后更新时间';

COMMENT ON COLUMN ledger_account.version
    IS '乐观锁版本号';

CREATE INDEX idx_ledger_account_owner
    ON ledger_account (owner_type, owner_id);

CREATE INDEX idx_ledger_account_owner_currency
    ON ledger_account (owner_type, owner_id, currency);

CREATE INDEX idx_ledger_account_status
    ON ledger_account (status);