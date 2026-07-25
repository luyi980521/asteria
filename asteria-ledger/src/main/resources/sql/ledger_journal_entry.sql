CREATE TABLE ledger_journal_entry (
    id                          VARCHAR(64) PRIMARY KEY,
    source_type                 VARCHAR(64) NOT NULL,
    source_id                   VARCHAR(128) NOT NULL,
    event_type                  VARCHAR(64) NOT NULL,
    event_id                    VARCHAR(64) NOT NULL,
    status                      VARCHAR(32) NOT NULL,
    posted_at                   TIMESTAMPTZ NULL,
    original_journal_entry_id   VARCHAR(64) NULL,
    reversing_journal_entry_id  VARCHAR(64) NULL,
    reversal_reason             VARCHAR(512) NULL,
    reversed_at                 TIMESTAMPTZ NULL,
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version                     BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT uk_journal_entry_event_id
        UNIQUE (event_id),

    CONSTRAINT ck_journal_entry_posted_at
        CHECK (
            (status = 'DRAFT' AND posted_at IS NULL)
            OR
            (status = 'POSTED' AND posted_at IS NOT NULL)
        )
);

COMMENT ON TABLE ledger_journal_entry IS '记账凭证表';

COMMENT ON COLUMN ledger_journal_entry.id
    IS '记账凭证ID';

COMMENT ON COLUMN ledger_journal_entry.source_type
    IS '来源业务对象类型，例如 PAYMENT、REFUND、SETTLEMENT';

COMMENT ON COLUMN ledger_journal_entry.source_id
    IS '来源业务对象ID';

COMMENT ON COLUMN ledger_journal_entry.event_type
    IS '触发记账的业务事件类型，例如 PAYMENT_CAPTURED';

COMMENT ON COLUMN ledger_journal_entry.event_id
    IS '业务事件唯一ID，用于幂等控制';

COMMENT ON COLUMN ledger_journal_entry.status
    IS '记账凭证状态：DRAFT-草稿，POSTED-已入账';

COMMENT ON COLUMN ledger_journal_entry.posted_at
    IS '正式入账时间，草稿状态时为空';

COMMENT ON COLUMN ledger_journal_entry.original_journal_entry_id
    IS '原记账凭证ID；当前凭证为冲正凭证时，指向被冲正的原凭证';

COMMENT ON COLUMN ledger_journal_entry.reversing_journal_entry_id
    IS '冲正凭证ID；当前凭证已被冲正时，指向对应的冲正凭证';

COMMENT ON COLUMN ledger_journal_entry.reversal_reason
    IS '冲正原因';

COMMENT ON COLUMN ledger_journal_entry.reversed_at
    IS '原凭证被冲正的时间';

COMMENT ON COLUMN ledger_journal_entry.created_at
    IS '记录创建时间';

COMMENT ON COLUMN ledger_journal_entry.updated_at
    IS '记录最后更新时间';

COMMENT ON COLUMN ledger_journal_entry.version
    IS '乐观锁版本号';
