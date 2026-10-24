-- Reconciliation v1：沿用模块 resources/sql 的增量 DDL 目录，不执行 DROP 或覆盖已有数据。
-- 所有关联均为逻辑关联；payload、details、source_record_ids 等序列化内容仅使用 TEXT。
CREATE TABLE reconciliation_case (
    id BIGINT PRIMARY KEY,
    rule_code VARCHAR(64) NOT NULL,
    business_key VARCHAR(256) NOT NULL,
    difference_type VARCHAR(32) NOT NULL,
    run_id BIGINT NOT NULL,
    match_result_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    assignee VARCHAR(128),
    details TEXT,
    resolution TEXT,
    closed_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,

    -- 应用生成的雪花主键必须为正数。
    CONSTRAINT ck_reconciliation_case_id CHECK (id > 0),

    -- 业务短文本不能为空白。
    CONSTRAINT ck_reconciliation_case_rule_code CHECK (rule_code IS NULL OR length(btrim(rule_code)) > 0),

    -- 业务短文本不能为空白。
    CONSTRAINT ck_reconciliation_case_business_key CHECK (business_key IS NULL OR length(btrim(business_key)) > 0),

    -- 限制为领域枚举中定义的取值。
    CONSTRAINT ck_reconciliation_case_difference_type CHECK (difference_type IN ('MISSING_PAYMENT', 'MISSING_LEDGER', 'MISSING_SETTLEMENT', 'AMOUNT_MISMATCH', 'CURRENCY_MISMATCH', 'STATUS_MISMATCH', 'POSTING_MISMATCH', 'DUPLICATE_RECORD', 'UNEXPECTED_RECORD')),

    -- 只保留逻辑关联，关联 ID 必须为正数，不建立物理外键。
    CONSTRAINT ck_reconciliation_case_run_id CHECK (run_id > 0),

    -- 只保留逻辑关联，关联 ID 必须为正数，不建立物理外键。
    CONSTRAINT ck_reconciliation_case_match_result_id CHECK (match_result_id > 0),

    -- 限制为领域枚举中定义的取值。
    CONSTRAINT ck_reconciliation_case_status CHECK (status IN ('OPEN', 'INVESTIGATING', 'RESOLVED', 'IGNORED')),

    -- 业务短文本不能为空白。
    CONSTRAINT ck_reconciliation_case_assignee CHECK (assignee IS NULL OR length(btrim(assignee)) > 0),

    -- 版本、宽限期或业务键计数不得为负数。
    CONSTRAINT ck_reconciliation_case_version CHECK (version >= 0),

    -- 审计更新时间不得早于创建时间。
    CONSTRAINT ck_reconciliation_case_audit_time CHECK (updated_at >= created_at),

    -- 调查中的工单必须具有非空责任人标识。
    CONSTRAINT ck_reconciliation_case_investigator CHECK (status <> 'INVESTIGATING' OR assignee IS NOT NULL),

    -- 活动工单无关闭信息，终态必须提供关闭时间与结论。
    CONSTRAINT ck_reconciliation_case_closure CHECK ((status IN ('OPEN', 'INVESTIGATING') AND closed_at IS NULL AND resolution IS NULL)
        OR (status IN ('RESOLVED', 'IGNORED') AND closed_at IS NOT NULL AND resolution IS NOT NULL AND length(btrim(resolution)) > 0)),

    -- 关闭时间位于工单审计时间范围内。
    CONSTRAINT ck_reconciliation_case_closed_time CHECK (closed_at IS NULL OR (closed_at >= created_at AND closed_at <= updated_at))
);

COMMENT ON TABLE reconciliation_case IS '跨运行跟踪的差异工单；OPEN、INVESTIGATING 属于活动状态，解决或忽略后允许后续新建。';
COMMENT ON COLUMN reconciliation_case.id IS '主键，由 DistributedIdGenerator 生成正数雪花 ID';
COMMENT ON COLUMN reconciliation_case.rule_code IS '稳定规则编码，逻辑关联 reconciliation_rule.rule_code';
COMMENT ON COLUMN reconciliation_case.business_key IS '规则约定的跨来源业务关联键；须包含必要的租户或渠道命名空间';
COMMENT ON COLUMN reconciliation_case.difference_type IS '工单差异类型，参与活动工单唯一性';
COMMENT ON COLUMN reconciliation_case.run_id IS '首次发现差异的运行 ID，逻辑关联 reconciliation_run.id';
COMMENT ON COLUMN reconciliation_case.match_result_id IS '首次差异结论 ID，逻辑关联 reconciliation_match_result.id';
COMMENT ON COLUMN reconciliation_case.status IS 'OPEN、INVESTIGATING、RESOLVED 或 IGNORED';
COMMENT ON COLUMN reconciliation_case.assignee IS '调查责任人标识，INVESTIGATING 必填';
COMMENT ON COLUMN reconciliation_case.details IS '使用 JsonUtils 序列化的 JSON 说明对象，以 TEXT 原样保存';
COMMENT ON COLUMN reconciliation_case.resolution IS '解决结论或忽略原因，终态必填';
COMMENT ON COLUMN reconciliation_case.closed_at IS '解决或忽略的时间，活动状态必须为空';
COMMENT ON COLUMN reconciliation_case.version IS '乐观锁版本；更新使用旧版本匹配并由数据库递增，更新后应重新读取';
COMMENT ON COLUMN reconciliation_case.created_at IS '记录创建时间，UTC 时间点';
COMMENT ON COLUMN reconciliation_case.updated_at IS '最后更新时间，UTC 时间点且不得早于创建时间';

-- 仅 OPEN 和 INVESTIGATING 占用唯一键，终态保留历史并允许后续新建。
CREATE UNIQUE INDEX uk_reconciliation_case_active
    ON reconciliation_case (rule_code, business_key, difference_type)
    WHERE status IN ('OPEN', 'INVESTIGATING');
COMMENT ON INDEX uk_reconciliation_case_active IS '仅 OPEN 和 INVESTIGATING 占用唯一键，终态保留历史并允许后续新建';

-- 按工单状态和发现时间稳定分页处理。
CREATE INDEX idx_reconciliation_case_status
    ON reconciliation_case (status, created_at, id);
COMMENT ON INDEX idx_reconciliation_case_status IS '按工单状态和发现时间稳定分页处理';

-- 按首次差异结果追溯对应工单。
CREATE INDEX idx_reconciliation_case_result
    ON reconciliation_case (match_result_id);
COMMENT ON INDEX idx_reconciliation_case_result IS '按首次差异结果追溯对应工单';
