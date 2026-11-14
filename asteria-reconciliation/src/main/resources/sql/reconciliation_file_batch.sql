-- 独立增量脚本，不修改六张核心表；由现有 SQL 发布流程执行一次。
CREATE TABLE reconciliation_file_batch (
    file_id BIGINT PRIMARY KEY,
    object_key TEXT NOT NULL,
    source_system VARCHAR(64) NOT NULL,
    record_type VARCHAR(64) NOT NULL,
    window_start TIMESTAMPTZ NOT NULL,
    window_end TIMESTAMPTZ NOT NULL,
    file_version BIGINT NOT NULL,
    schema_version INTEGER NOT NULL,
    sha256 VARCHAR(64) NOT NULL,
    record_count BIGINT NOT NULL,
    byte_size BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    failure_reason TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    -- 同一来源、路径与版本只登记一次，不覆盖原始摘要。
    CONSTRAINT uk_reconciliation_file_batch_delivery UNIQUE (source_system, object_key, file_version),
    CONSTRAINT ck_reconciliation_file_batch_id CHECK (file_id > 0),
    CONSTRAINT ck_reconciliation_file_batch_window CHECK (window_start < window_end),
    CONSTRAINT ck_reconciliation_file_batch_counts CHECK (file_version >= 0 AND schema_version > 0 AND record_count >= 0 AND byte_size >= 0),
    CONSTRAINT ck_reconciliation_file_batch_sha256 CHECK (sha256 ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ck_reconciliation_file_batch_text CHECK (length(btrim(source_system)) > 0 AND length(btrim(object_key)) > 0 AND length(btrim(record_type)) > 0),
    CONSTRAINT ck_reconciliation_file_batch_status CHECK (status IN ('CREATED','IMPORTING','COMPLETED','FAILED')),
    CONSTRAINT ck_reconciliation_file_batch_completion CHECK (
        (status IN ('CREATED','IMPORTING') AND completed_at IS NULL)
        OR (status IN ('COMPLETED','FAILED') AND completed_at IS NOT NULL AND completed_at >= created_at)),
    CONSTRAINT ck_reconciliation_file_batch_failure CHECK (status <> 'FAILED' OR (failure_reason IS NOT NULL AND length(btrim(failure_reason)) > 0))
);
COMMENT ON TABLE reconciliation_file_batch IS '独立文件导入批次元数据；不绑定对账 Run；事务与状态策略由文件导入服务实现';
COMMENT ON COLUMN reconciliation_file_batch.file_id IS '应用分配的文件主键';
COMMENT ON COLUMN reconciliation_file_batch.object_key IS '对象存储相对路径';
COMMENT ON COLUMN reconciliation_file_batch.source_system IS '来源系统';
COMMENT ON COLUMN reconciliation_file_batch.record_type IS '文件记录类型';
COMMENT ON COLUMN reconciliation_file_batch.window_start IS '业务窗口起点，包含';
COMMENT ON COLUMN reconciliation_file_batch.window_end IS '业务窗口终点，不包含';
COMMENT ON COLUMN reconciliation_file_batch.file_version IS '文件交付版本，不代表来源业务版本';
COMMENT ON COLUMN reconciliation_file_batch.schema_version IS 'CSV 格式版本';
COMMENT ON COLUMN reconciliation_file_batch.sha256 IS '文件原始字节的 SHA-256 小写十六进制摘要';
COMMENT ON COLUMN reconciliation_file_batch.record_count IS '不含表头的记录数';
COMMENT ON COLUMN reconciliation_file_batch.byte_size IS '文件字节数';
COMMENT ON COLUMN reconciliation_file_batch.status IS '导入状态 CREATED、IMPORTING、COMPLETED、FAILED';
COMMENT ON COLUMN reconciliation_file_batch.failure_reason IS '失败说明，可空';
COMMENT ON COLUMN reconciliation_file_batch.created_at IS '元数据创建时间';
COMMENT ON COLUMN reconciliation_file_batch.completed_at IS '导入结束时间，可空';
CREATE INDEX idx_reconciliation_file_batch_status ON reconciliation_file_batch (status, created_at);
COMMENT ON INDEX idx_reconciliation_file_batch_status IS '按导入状态和创建时间查询文件批次';