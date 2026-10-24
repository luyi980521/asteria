-- Reconciliation v1：沿用模块 resources/sql 的增量 DDL 目录，不执行 DROP 或覆盖已有数据。
-- 所有关联均为逻辑关联；payload、details、source_record_ids 等序列化内容仅使用 TEXT。
CREATE TABLE reconciliation_match_result (
    id BIGINT PRIMARY KEY,
    run_id BIGINT NOT NULL,
    rule_code VARCHAR(64) NOT NULL,
    business_key VARCHAR(256) NOT NULL,
    status VARCHAR(32) NOT NULL,
    difference_type VARCHAR(32),
    expected_amount NUMERIC(38, 18),
    expected_currency VARCHAR(16),
    actual_amount NUMERIC(38, 18),
    actual_currency VARCHAR(16),
    source_record_ids TEXT NOT NULL,
    details TEXT,
    created_at TIMESTAMPTZ NOT NULL,

    -- 应用生成的雪花主键必须为正数。
    CONSTRAINT ck_reconciliation_match_result_id CHECK (id > 0),

    -- 只保留逻辑关联，关联 ID 必须为正数，不建立物理外键。
    CONSTRAINT ck_reconciliation_match_result_run_id CHECK (run_id > 0),

    -- 业务短文本不能为空白。
    CONSTRAINT ck_reconciliation_match_result_rule_code CHECK (rule_code IS NULL OR length(btrim(rule_code)) > 0),

    -- 业务短文本不能为空白。
    CONSTRAINT ck_reconciliation_match_result_business_key CHECK (business_key IS NULL OR length(btrim(business_key)) > 0),

    -- 限制为领域枚举中定义的取值。
    CONSTRAINT ck_reconciliation_match_result_status CHECK (status IN ('MATCHED', 'MISMATCHED', 'MISSING', 'PENDING', 'DUPLICATED')),

    -- 限制为领域枚举中定义的取值。
    CONSTRAINT ck_reconciliation_match_result_difference_type CHECK (difference_type IN ('MISSING_PAYMENT', 'MISSING_LEDGER', 'MISSING_SETTLEMENT', 'AMOUNT_MISMATCH', 'CURRENCY_MISMATCH', 'STATUS_MISMATCH', 'POSTING_MISMATCH', 'DUPLICATE_RECORD', 'UNEXPECTED_RECORD')),

    -- Money 成对保存金额和币种，允许零，未知金额整体为空。
    CONSTRAINT ck_reconciliation_match_result_expected_amount CHECK ((expected_amount IS NULL AND expected_currency IS NULL) OR (expected_amount IS NOT NULL AND expected_currency IS NOT NULL AND expected_amount >= 0 AND expected_currency ~ '^[A-Z]{3}$')),

    -- Money 成对保存金额和币种，允许零，未知金额整体为空。
    CONSTRAINT ck_reconciliation_match_result_actual_amount CHECK ((actual_amount IS NULL AND actual_currency IS NULL) OR (actual_amount IS NOT NULL AND actual_currency IS NOT NULL AND actual_amount >= 0 AND actual_currency ~ '^[A-Z]{3}$')),

    -- 必填文本不允许空白；JSON 格式由领域层通过 JsonUtils 校验。
    CONSTRAINT ck_reconciliation_match_result_source_record_ids CHECK (length(btrim(source_record_ids)) > 0),

    -- 同一运行、规则与业务键仅保存一个不可变匹配结果。
    CONSTRAINT uk_reconciliation_match_result_business UNIQUE (run_id, rule_code, business_key),

    -- 差异分类与匹配状态一致，待确认结果不得携带已确认差异。
    CONSTRAINT ck_reconciliation_match_result_difference CHECK ((status IN ('MATCHED', 'PENDING') AND difference_type IS NULL)
        OR (status = 'MISSING' AND difference_type IS NOT NULL AND difference_type IN ('MISSING_PAYMENT', 'MISSING_LEDGER', 'MISSING_SETTLEMENT'))
        OR (status = 'DUPLICATED' AND difference_type = 'DUPLICATE_RECORD' AND difference_type IS NOT NULL)
        OR (status = 'MISMATCHED' AND difference_type IS NOT NULL AND difference_type IN ('AMOUNT_MISMATCH', 'CURRENCY_MISMATCH', 'STATUS_MISMATCH', 'POSTING_MISMATCH', 'UNEXPECTED_RECORD')))
);

COMMENT ON TABLE reconciliation_match_result IS '运行中单一规则与业务键的不可变核对结论；重跑使用新的 Run，PENDING 不创建 Case。';
COMMENT ON COLUMN reconciliation_match_result.id IS '主键，由 DistributedIdGenerator 生成正数雪花 ID';
COMMENT ON COLUMN reconciliation_match_result.run_id IS '所属运行 ID，逻辑关联 reconciliation_run.id，无物理外键';
COMMENT ON COLUMN reconciliation_match_result.rule_code IS '稳定规则编码，逻辑关联 reconciliation_rule.rule_code';
COMMENT ON COLUMN reconciliation_match_result.business_key IS '规则约定的跨来源业务关联键；须包含必要的租户或渠道命名空间';
COMMENT ON COLUMN reconciliation_match_result.status IS '匹配状态，决定是否允许存在差异类型';
COMMENT ON COLUMN reconciliation_match_result.difference_type IS '差异类型；MATCHED、PENDING 时为空，其余状态必填';
COMMENT ON COLUMN reconciliation_match_result.expected_amount IS '基准侧金额，来源缺失时允许为空';
COMMENT ON COLUMN reconciliation_match_result.expected_currency IS '基准侧金额，来源缺失时允许为空所对应的币种；金额为空时币种也必须为空';
COMMENT ON COLUMN reconciliation_match_result.actual_amount IS '比较侧金额，可具有不同币种，来源缺失时允许为空';
COMMENT ON COLUMN reconciliation_match_result.actual_currency IS '比较侧金额，可具有不同币种，来源缺失时允许为空所对应的币种；金额为空时币种也必须为空';
COMMENT ON COLUMN reconciliation_match_result.source_record_ids IS 'JsonUtils 序列化的非空正数快照主键数组；逻辑关联 source_record.id，禁止重复 ID';
COMMENT ON COLUMN reconciliation_match_result.details IS '使用 JsonUtils 序列化的 JSON 说明对象，以 TEXT 原样保存';
COMMENT ON COLUMN reconciliation_match_result.created_at IS '记录创建时间，UTC 时间点';

-- 按运行和结论状态稳定分页，避免 Run 加载全部结果。
CREATE INDEX idx_reconciliation_match_result_run_status
    ON reconciliation_match_result (run_id, status, id);
COMMENT ON INDEX idx_reconciliation_match_result_run_status IS '按运行和结论状态稳定分页，避免 Run 加载全部结果';

-- 按规则业务键追踪跨运行核对历史。
CREATE INDEX idx_reconciliation_match_result_business_history
    ON reconciliation_match_result (rule_code, business_key, created_at DESC);
COMMENT ON INDEX idx_reconciliation_match_result_business_history IS '按规则业务键追踪跨运行核对历史';
