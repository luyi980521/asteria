-- Reconciliation v1：沿用模块 resources/sql 的增量 DDL 目录，不执行 DROP 或覆盖已有数据。
-- 所有关联均为逻辑关联；payload、details、source_record_ids 等序列化内容仅使用 TEXT。
CREATE TABLE reconciliation_run (
    id BIGINT PRIMARY KEY,
    rule_code VARCHAR(64) NOT NULL,
    rule_version BIGINT NOT NULL,
    type VARCHAR(32) NOT NULL,
    rule_snapshot TEXT NOT NULL,
    window_start TIMESTAMPTZ NOT NULL,
    window_end TIMESTAMPTZ NOT NULL,
    source_cutoff_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(32) NOT NULL,
    total_count BIGINT NOT NULL DEFAULT 0,
    matched_count BIGINT NOT NULL DEFAULT 0,
    mismatched_count BIGINT NOT NULL DEFAULT 0,
    missing_count BIGINT NOT NULL DEFAULT 0,
    pending_count BIGINT NOT NULL DEFAULT 0,
    duplicated_count BIGINT NOT NULL DEFAULT 0,
    started_at TIMESTAMPTZ,
    finished_at TIMESTAMPTZ,
    failure_reason TEXT,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,

    -- 应用生成的雪花主键必须为正数。
    CONSTRAINT ck_reconciliation_run_id CHECK (id > 0),

    -- 业务短文本不能为空白。
    CONSTRAINT ck_reconciliation_run_rule_code CHECK (rule_code IS NULL OR length(btrim(rule_code)) > 0),

    -- 版本、宽限期或业务键计数不得为负数。
    CONSTRAINT ck_reconciliation_run_rule_version CHECK (rule_version >= 0),

    -- 限制为领域枚举中定义的取值。
    CONSTRAINT ck_reconciliation_run_type CHECK (type IN ('PAYMENT_LEDGER', 'SETTLEMENT_LEDGER')),

    -- 必填文本不允许空白；JSON 格式由领域层通过 JsonUtils 校验。
    CONSTRAINT ck_reconciliation_run_rule_snapshot CHECK (length(btrim(rule_snapshot)) > 0),

    -- 限制为领域枚举中定义的取值。
    CONSTRAINT ck_reconciliation_run_status CHECK (status IN ('CREATED', 'RUNNING', 'COMPLETED', 'FAILED')),

    -- 版本、宽限期或业务键计数不得为负数。
    CONSTRAINT ck_reconciliation_run_total_count CHECK (total_count >= 0),

    -- 版本、宽限期或业务键计数不得为负数。
    CONSTRAINT ck_reconciliation_run_matched_count CHECK (matched_count >= 0),

    -- 版本、宽限期或业务键计数不得为负数。
    CONSTRAINT ck_reconciliation_run_mismatched_count CHECK (mismatched_count >= 0),

    -- 版本、宽限期或业务键计数不得为负数。
    CONSTRAINT ck_reconciliation_run_missing_count CHECK (missing_count >= 0),

    -- 版本、宽限期或业务键计数不得为负数。
    CONSTRAINT ck_reconciliation_run_pending_count CHECK (pending_count >= 0),

    -- 版本、宽限期或业务键计数不得为负数。
    CONSTRAINT ck_reconciliation_run_duplicated_count CHECK (duplicated_count >= 0),

    -- 版本、宽限期或业务键计数不得为负数。
    CONSTRAINT ck_reconciliation_run_version CHECK (version >= 0),

    -- 审计更新时间不得早于创建时间。
    CONSTRAINT ck_reconciliation_run_audit_time CHECK (updated_at >= created_at),

    -- 窗口为左闭右开区间，采集可见性截止点不得早于窗口终点或晚于创建。
    CONSTRAINT ck_reconciliation_run_window CHECK (window_start < window_end AND source_cutoff_at >= window_end AND source_cutoff_at <= created_at),

    -- 互斥匹配分类计数之和必须等于业务键总数，使用 NUMERIC 避免 BIGINT 求和溢出。
    CONSTRAINT ck_reconciliation_run_counts CHECK (total_count::NUMERIC = matched_count::NUMERIC + mismatched_count::NUMERIC + missing_count::NUMERIC + pending_count::NUMERIC + duplicated_count::NUMERIC),

    -- 开始和结束时间与运行状态一致。
    CONSTRAINT ck_reconciliation_run_lifecycle CHECK ((status = 'CREATED' AND started_at IS NULL AND finished_at IS NULL)
        OR (status = 'RUNNING' AND started_at IS NOT NULL AND finished_at IS NULL)
        OR (status IN ('COMPLETED', 'FAILED') AND started_at IS NOT NULL AND finished_at IS NOT NULL)),

    -- v1 仅完成运行时写入统计，失败或未完成状态不伪造完整统计。
    CONSTRAINT ck_reconciliation_run_state_counts CHECK (status = 'COMPLETED' OR total_count = 0),

    -- 只有失败运行保存非空失败原因。
    CONSTRAINT ck_reconciliation_run_failure CHECK ((status = 'FAILED' AND failure_reason IS NOT NULL AND length(btrim(failure_reason)) > 0)
        OR (status <> 'FAILED' AND failure_reason IS NULL)),

    -- 开始时间位于审计时间范围内。
    CONSTRAINT ck_reconciliation_run_started_time CHECK (started_at IS NULL OR (started_at >= created_at AND started_at <= updated_at)),

    -- 终态时间不得早于开始时间。
    CONSTRAINT ck_reconciliation_run_finished_time CHECK (finished_at IS NULL OR (finished_at >= started_at AND finished_at <= updated_at))
);

COMMENT ON TABLE reconciliation_run IS '一次规则执行的状态和计数；窗口为 [windowStart, windowEnd)，不持有来源及结果集合。';
COMMENT ON COLUMN reconciliation_run.id IS '主键，由 DistributedIdGenerator 生成正数雪花 ID';
COMMENT ON COLUMN reconciliation_run.rule_code IS '稳定规则编码，逻辑关联 reconciliation_rule.rule_code';
COMMENT ON COLUMN reconciliation_run.rule_version IS '运行选用的规则乐观锁版本，用于追溯规则配置';
COMMENT ON COLUMN reconciliation_run.type IS '运行选用的对账类型快照';
COMMENT ON COLUMN reconciliation_run.rule_snapshot IS '使用 JsonUtils 序列化的运行时完整规则 JSON 快照';
COMMENT ON COLUMN reconciliation_run.window_start IS '对账业务窗口起点，包含此时刻';
COMMENT ON COLUMN reconciliation_run.window_end IS '对账业务窗口终点，不包含此时刻';
COMMENT ON COLUMN reconciliation_run.source_cutoff_at IS '采集可见性截止时间，只使用 created_at 小于等于此值的来源快照';
COMMENT ON COLUMN reconciliation_run.status IS 'CREATED、RUNNING、COMPLETED 或 FAILED；终态不可重启';
COMMENT ON COLUMN reconciliation_run.total_count IS '本次已评估的业务键总数，不是来源记录数';
COMMENT ON COLUMN reconciliation_run.matched_count IS 'matched 状态的业务键数量';
COMMENT ON COLUMN reconciliation_run.mismatched_count IS 'mismatched 状态的业务键数量';
COMMENT ON COLUMN reconciliation_run.missing_count IS 'missing 状态的业务键数量';
COMMENT ON COLUMN reconciliation_run.pending_count IS 'pending 状态的业务键数量';
COMMENT ON COLUMN reconciliation_run.duplicated_count IS 'duplicated 状态的业务键数量';
COMMENT ON COLUMN reconciliation_run.started_at IS '开始运行时间，仅 RUNNING 或终态存在';
COMMENT ON COLUMN reconciliation_run.finished_at IS '运行完成或失败时间，只有终态存在';
COMMENT ON COLUMN reconciliation_run.failure_reason IS '失败原因；FAILED 状态必填';
COMMENT ON COLUMN reconciliation_run.version IS '乐观锁版本；更新使用旧版本匹配并由数据库递增，更新后应重新读取';
COMMENT ON COLUMN reconciliation_run.created_at IS '记录创建时间，UTC 时间点';
COMMENT ON COLUMN reconciliation_run.updated_at IS '最后更新时间，UTC 时间点且不得早于创建时间';

-- 按规则和窗口查找运行历史，重跑可保留多个 Run。
CREATE INDEX idx_reconciliation_run_rule_window
    ON reconciliation_run (rule_code, window_start, window_end);
COMMENT ON INDEX idx_reconciliation_run_rule_window IS '按规则和窗口查找运行历史，重跑可保留多个 Run';

-- 按运行状态排查长时间未结束的任务。
CREATE INDEX idx_reconciliation_run_status
    ON reconciliation_run (status, created_at, id);
COMMENT ON INDEX idx_reconciliation_run_status IS '按运行状态排查长时间未结束的任务';
