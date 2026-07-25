DROP TABLE IF EXISTS ledger_posting;
CREATE TABLE ledger_posting (
    id                  BIGINT PRIMARY KEY,
    journal_entry_id    BIGINT NOT NULL,
    ledger_account_id   BIGINT NOT NULL,
    amount              NUMERIC(38, 18) NOT NULL,
    currency            VARCHAR(16) NOT NULL,
    direction           VARCHAR(16) NOT NULL,
    sequence_no         INTEGER NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT ck_posting_id_positive
        CHECK (id > 0),

    CONSTRAINT ck_posting_journal_entry_id_positive
        CHECK (journal_entry_id > 0),

    CONSTRAINT ck_posting_ledger_account_id_positive
        CHECK (ledger_account_id > 0),

    CONSTRAINT ck_posting_amount_positive
        CHECK (amount > 0),

    CONSTRAINT ck_posting_currency_not_blank
        CHECK (length(trim(currency)) > 0),

    CONSTRAINT ck_posting_direction
        CHECK (direction IN ('DEBIT', 'CREDIT')),

    CONSTRAINT ck_posting_sequence_positive
        CHECK (sequence_no > 0),

    CONSTRAINT uk_posting_sequence
        UNIQUE (journal_entry_id, sequence_no)
);

COMMENT ON TABLE ledger_posting IS '记账分录表';

COMMENT ON COLUMN ledger_posting.id
    IS '分录ID';

COMMENT ON COLUMN ledger_posting.journal_entry_id
    IS '所属记账凭证ID';

COMMENT ON COLUMN ledger_posting.ledger_account_id
    IS '账本账户ID';

COMMENT ON COLUMN ledger_posting.amount
    IS '分录金额，必须大于0；借贷方向由direction字段表达';

COMMENT ON COLUMN ledger_posting.currency
    IS '币种代码，例如 CNY、USD、EUR';

COMMENT ON COLUMN ledger_posting.direction
    IS '借贷方向：DEBIT-借方，CREDIT-贷方';

COMMENT ON COLUMN ledger_posting.sequence_no
    IS '分录在记账凭证中的顺序号';

COMMENT ON COLUMN ledger_posting.created_at
    IS '记录创建时间';