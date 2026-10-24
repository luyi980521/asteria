-- Reconciliation v1：沿用模块 resources/sql 的增量 DDL 目录，不执行 DROP 或覆盖已有数据。
-- 所有关联均为逻辑关联；payload、details、source_record_ids 等序列化内容仅使用 TEXT。
CREATE TABLE reconciliation_case_action (
    id BIGINT PRIMARY KEY,
    case_id BIGINT NOT NULL,
    action_type VARCHAR(32) NOT NULL,
    operator_type VARCHAR(32) NOT NULL,
    operator_id VARCHAR(128) NOT NULL,
    from_status VARCHAR(32),
    to_status VARCHAR(32) NOT NULL,
    reason TEXT NOT NULL,
    details TEXT,
    created_at TIMESTAMPTZ NOT NULL,

    -- 应用生成的雪花主键必须为正数。
    CONSTRAINT ck_reconciliation_case_action_id CHECK (id > 0),

    -- 只保留逻辑关联，关联 ID 必须为正数，不建立物理外键。
    CONSTRAINT ck_reconciliation_case_action_case_id CHECK (case_id > 0),

    -- 限制为领域枚举中定义的取值。
    CONSTRAINT ck_reconciliation_case_action_action_type CHECK (action_type IN ('OPEN', 'INVESTIGATE', 'RESOLVE', 'IGNORE', 'COMMENT')),

    -- 限制为领域枚举中定义的取值。
    CONSTRAINT ck_reconciliation_case_action_operator_type CHECK (operator_type IN ('SYSTEM', 'HUMAN')),

    -- 业务短文本不能为空白。
    CONSTRAINT ck_reconciliation_case_action_operator_id CHECK (operator_id IS NULL OR length(btrim(operator_id)) > 0),

    -- 限制为领域枚举中定义的取值。
    CONSTRAINT ck_reconciliation_case_action_from_status CHECK (from_status IN ('OPEN', 'INVESTIGATING', 'RESOLVED', 'IGNORED')),

    -- 限制为领域枚举中定义的取值。
    CONSTRAINT ck_reconciliation_case_action_to_status CHECK (to_status IN ('OPEN', 'INVESTIGATING', 'RESOLVED', 'IGNORED')),

    -- 所有审计动作必须说明原因，尤其是解决或忽略。
    CONSTRAINT ck_reconciliation_case_action_reason CHECK (length(btrim(reason)) > 0),

    -- 审计动作的前后状态必须描述合法转换，COMMENT 不改变状态。
    CONSTRAINT ck_reconciliation_case_action_transition CHECK ((action_type = 'OPEN' AND from_status IS NULL AND to_status = 'OPEN')
        OR (action_type = 'INVESTIGATE' AND from_status IS NOT NULL AND from_status = 'OPEN' AND to_status = 'INVESTIGATING')
        OR (action_type = 'RESOLVE' AND from_status IS NOT NULL AND from_status IN ('OPEN', 'INVESTIGATING') AND to_status = 'RESOLVED')
        OR (action_type = 'IGNORE' AND from_status IS NOT NULL AND from_status IN ('OPEN', 'INVESTIGATING') AND to_status = 'IGNORED')
        OR (action_type = 'COMMENT' AND from_status IS NOT NULL AND from_status = to_status))
);

COMMENT ON TABLE reconciliation_case_action IS '工单追加式审计记录；仅记录操作，不执行资金修复，必须与工单变更在同一事务保存。';
COMMENT ON COLUMN reconciliation_case_action.id IS '主键，由 DistributedIdGenerator 生成正数雪花 ID';
COMMENT ON COLUMN reconciliation_case_action.case_id IS '工单 ID，逻辑关联 reconciliation_case.id，无物理外键';
COMMENT ON COLUMN reconciliation_case_action.action_type IS '审计动作，限定合法的前后状态组合';
COMMENT ON COLUMN reconciliation_case_action.operator_type IS '操作主体类型 SYSTEM 或 HUMAN';
COMMENT ON COLUMN reconciliation_case_action.operator_id IS '可追溯的系统或人工主体标识，系统操作也必填';
COMMENT ON COLUMN reconciliation_case_action.from_status IS '操作前状态，仅 OPEN 动作允许为空';
COMMENT ON COLUMN reconciliation_case_action.to_status IS '操作后状态；COMMENT 时必须与前状态相同';
COMMENT ON COLUMN reconciliation_case_action.reason IS '操作说明，解决和忽略动作必须给出原因';
COMMENT ON COLUMN reconciliation_case_action.details IS '使用 JsonUtils 序列化的 JSON 说明对象，以 TEXT 原样保存';
COMMENT ON COLUMN reconciliation_case_action.created_at IS '记录创建时间，UTC 时间点';

-- 按时间和主键还原单个工单的追加式审计历史。
CREATE INDEX idx_reconciliation_case_action_case_time
    ON reconciliation_case_action (case_id, created_at, id);
COMMENT ON INDEX idx_reconciliation_case_action_case_time IS '按时间和主键还原单个工单的追加式审计历史';
