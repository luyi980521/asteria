-- Reconciliation v1：沿用模块 resources/sql 的增量 DDL 目录，不执行 DROP 或覆盖已有数据。
-- 所有关联均为逻辑关联；payload、details、source_record_ids 等序列化内容仅使用 TEXT。
CREATE TABLE reconciliation_rule (
    id BIGINT PRIMARY KEY,
    rule_code VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    type VARCHAR(32) NOT NULL,
    left_source_type VARCHAR(32) NOT NULL,
    right_source_type VARCHAR(32) NOT NULL,
    enabled BOOLEAN NOT NULL,
    grace_period_seconds BIGINT NOT NULL,
    details TEXT,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,

    -- 应用生成的雪花主键必须为正数。
    CONSTRAINT ck_reconciliation_rule_id CHECK (id > 0),

    -- 业务短文本不能为空白。
    CONSTRAINT ck_reconciliation_rule_rule_code CHECK (rule_code IS NULL OR length(btrim(rule_code)) > 0),

    -- 业务短文本不能为空白。
    CONSTRAINT ck_reconciliation_rule_name CHECK (name IS NULL OR length(btrim(name)) > 0),

    -- 限制为领域枚举中定义的取值。
    CONSTRAINT ck_reconciliation_rule_type CHECK (type IN ('PAYMENT_LEDGER', 'SETTLEMENT_LEDGER')),

    -- 限制为领域枚举中定义的取值。
    CONSTRAINT ck_reconciliation_rule_left_source_type CHECK (left_source_type IN ('PAYMENT', 'LEDGER', 'SETTLEMENT', 'BALANCE', 'CHANNEL', 'BANK', 'FUND_TA')),

    -- 限制为领域枚举中定义的取值。
    CONSTRAINT ck_reconciliation_rule_right_source_type CHECK (right_source_type IN ('PAYMENT', 'LEDGER', 'SETTLEMENT', 'BALANCE', 'CHANNEL', 'BANK', 'FUND_TA')),

    -- 版本、宽限期或业务键计数不得为负数。
    CONSTRAINT ck_reconciliation_rule_grace_period_seconds CHECK (grace_period_seconds >= 0),

    -- 版本、宽限期或业务键计数不得为负数。
    CONSTRAINT ck_reconciliation_rule_version CHECK (version >= 0),

    -- 审计更新时间不得早于创建时间。
    CONSTRAINT ck_reconciliation_rule_audit_time CHECK (updated_at >= created_at),

    -- 稳定规则编码全局唯一，版本在原记录上递增。
    CONSTRAINT uk_reconciliation_rule_code UNIQUE (rule_code),

    -- v1 对账仅支持支付或结算与账本的固定来源组合。
    CONSTRAINT ck_reconciliation_rule_source_pair CHECK (right_source_type = 'LEDGER' AND ((type = 'PAYMENT_LEDGER' AND left_source_type = 'PAYMENT') OR (type = 'SETTLEMENT_LEDGER' AND left_source_type = 'SETTLEMENT')))
);

COMMENT ON TABLE reconciliation_rule IS '对账规则定义；编码稳定，配置变更递增版本，Run 保存版本与快照以便审计。';
COMMENT ON COLUMN reconciliation_rule.id IS '主键，由 DistributedIdGenerator 生成正数雪花 ID';
COMMENT ON COLUMN reconciliation_rule.rule_code IS '稳定规则编码，逻辑关联 reconciliation_rule.rule_code';
COMMENT ON COLUMN reconciliation_rule.name IS '业务可读的规则名称';
COMMENT ON COLUMN reconciliation_rule.type IS '对账关系类型';
COMMENT ON COLUMN reconciliation_rule.left_source_type IS '基准来源；v1 为 PAYMENT 或 SETTLEMENT';
COMMENT ON COLUMN reconciliation_rule.right_source_type IS '被核对来源；v1 必须为 LEDGER';
COMMENT ON COLUMN reconciliation_rule.enabled IS '是否允许创建新的对账运行';
COMMENT ON COLUMN reconciliation_rule.grace_period_seconds IS '等待异步入账的宽限秒数，非负；窗口结束加宽限后才可判定缺失';
COMMENT ON COLUMN reconciliation_rule.details IS '使用 JsonUtils 序列化的 JSON 说明对象，以 TEXT 原样保存';
COMMENT ON COLUMN reconciliation_rule.version IS '乐观锁版本；更新使用旧版本匹配并由数据库递增，更新后应重新读取';
COMMENT ON COLUMN reconciliation_rule.created_at IS '记录创建时间，UTC 时间点';
COMMENT ON COLUMN reconciliation_rule.updated_at IS '最后更新时间，UTC 时间点且不得早于创建时间';
