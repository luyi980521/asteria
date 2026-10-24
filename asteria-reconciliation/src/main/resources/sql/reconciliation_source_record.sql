-- Reconciliation v1：沿用模块 resources/sql 的增量 DDL 目录，不执行 DROP 或覆盖已有数据。
-- 所有关联均为逻辑关联；payload、details、source_record_ids 等序列化内容仅使用 TEXT。
CREATE TABLE reconciliation_source_record (
    id BIGINT PRIMARY KEY,
    source_type VARCHAR(32) NOT NULL,
    source_record_id VARCHAR(128) NOT NULL,
    source_version BIGINT NOT NULL,
    business_key VARCHAR(256) NOT NULL,
    amount NUMERIC(38, 18),
    currency VARCHAR(16),
    source_status VARCHAR(64) NOT NULL,
    posting_status VARCHAR(64),
    occurred_at TIMESTAMPTZ NOT NULL,
    payload TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,

    -- 应用生成的雪花主键必须为正数。
    CONSTRAINT ck_reconciliation_source_record_id CHECK (id > 0),

    -- 限制为领域枚举中定义的取值。
    CONSTRAINT ck_reconciliation_source_record_source_type CHECK (source_type IN ('PAYMENT', 'LEDGER', 'SETTLEMENT', 'BALANCE', 'CHANNEL', 'BANK', 'FUND_TA')),

    -- 业务短文本不能为空白。
    CONSTRAINT ck_reconciliation_source_record_source_record_id CHECK (source_record_id IS NULL OR length(btrim(source_record_id)) > 0),

    -- 版本、宽限期或业务键计数不得为负数。
    CONSTRAINT ck_reconciliation_source_record_source_version CHECK (source_version >= 0),

    -- 业务短文本不能为空白。
    CONSTRAINT ck_reconciliation_source_record_business_key CHECK (business_key IS NULL OR length(btrim(business_key)) > 0),

    -- Money 成对保存金额和币种，允许零，未知金额整体为空。
    CONSTRAINT ck_reconciliation_source_record_amount CHECK ((amount IS NULL AND currency IS NULL) OR (amount IS NOT NULL AND currency IS NOT NULL AND amount >= 0 AND currency ~ '^[A-Z]{3}$')),

    -- 业务短文本不能为空白。
    CONSTRAINT ck_reconciliation_source_record_source_status CHECK (source_status IS NULL OR length(btrim(source_status)) > 0),

    -- 业务短文本不能为空白。
    CONSTRAINT ck_reconciliation_source_record_posting_status CHECK (posting_status IS NULL OR length(btrim(posting_status)) > 0),

    -- 必填文本不允许空白；JSON 格式由领域层通过 JsonUtils 校验。
    CONSTRAINT ck_reconciliation_source_record_payload CHECK (length(btrim(payload)) > 0),

    -- 来源三元组保证重复采集幂等，同一业务版本不得覆盖。
    CONSTRAINT uk_reconciliation_source_record_source UNIQUE (source_type, source_record_id, source_version),

    -- 业务发生时间不得晚于快照采集时间。
    CONSTRAINT ck_reconciliation_source_record_occurred_time CHECK (occurred_at <= created_at)
);

COMMENT ON TABLE reconciliation_source_record IS '来源业务的不可变标准化快照；同一来源记录不同版本可并存，不依附单个 Run。';
COMMENT ON COLUMN reconciliation_source_record.id IS '主键，由 DistributedIdGenerator 生成正数雪花 ID';
COMMENT ON COLUMN reconciliation_source_record.source_type IS '来源系统类型，与原始标识和版本共同构成幂等键';
COMMENT ON COLUMN reconciliation_source_record.source_record_id IS '来源系统原始记录 ID，跨渠道时需带命名空间';
COMMENT ON COLUMN reconciliation_source_record.source_version IS '来源记录的单调非负版本；无原生版本时采集方须提供稳定映射';
COMMENT ON COLUMN reconciliation_source_record.business_key IS '规则约定的跨来源业务关联键；须包含必要的租户或渠道命名空间';
COMMENT ON COLUMN reconciliation_source_record.amount IS '标准化金额，允许零；未知金额整体为空';
COMMENT ON COLUMN reconciliation_source_record.currency IS '标准化金额，允许零；未知金额整体为空所对应的币种；金额为空时币种也必须为空';
COMMENT ON COLUMN reconciliation_source_record.source_status IS '原始业务状态，保留来源语义，由规则解释';
COMMENT ON COLUMN reconciliation_source_record.posting_status IS '可选的入账或记账状态，由规则解释';
COMMENT ON COLUMN reconciliation_source_record.occurred_at IS '来源业务发生时间，按此字段落入左闭右开对账窗口';
COMMENT ON COLUMN reconciliation_source_record.payload IS 'JsonUtils 序列化的原始业务快照 JSON 对象；不可修改';
COMMENT ON COLUMN reconciliation_source_record.created_at IS '记录创建时间，UTC 时间点';

-- 按来源和左闭右开业务窗口扫描，id 用于稳定分页。
CREATE INDEX idx_reconciliation_source_record_window
    ON reconciliation_source_record (source_type, occurred_at, id);
COMMENT ON INDEX idx_reconciliation_source_record_window IS '按来源和左闭右开业务窗口扫描，id 用于稳定分页';

-- 跨来源按业务键查询并选取截止时间内可见版本。
CREATE INDEX idx_reconciliation_source_record_business
    ON reconciliation_source_record (business_key, source_type, source_version DESC);
COMMENT ON INDEX idx_reconciliation_source_record_business IS '跨来源按业务键查询并选取截止时间内可见版本';
