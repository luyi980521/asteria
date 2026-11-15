-- 完整建表定义，与 ReconciliationFileBatchDO 的字段及定义顺序对应，不自动执行。
CREATE TABLE reconciliation_file_batch (
    id BIGINT PRIMARY KEY,
    batch_id BIGINT,
    source_system VARCHAR(64) NOT NULL,
    object_key TEXT NOT NULL,
    record_type VARCHAR(64) NOT NULL,
    file_version BIGINT NOT NULL,
    schema_version INTEGER NOT NULL,
    business_date DATE,
    window_start TIMESTAMPTZ NOT NULL,
    window_end TIMESTAMPTZ NOT NULL,
    cutoff_at TIMESTAMPTZ,
    sha256 VARCHAR(64) NOT NULL,
    byte_size BIGINT NOT NULL,
    record_count BIGINT NOT NULL,
    actual_record_count BIGINT,
    status VARCHAR(32) NOT NULL,
    failure_reason TEXT,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- 同一来源、路径与版本只登记一次，不覆盖原始摘要。
    CONSTRAINT uk_reconciliation_file_batch_delivery UNIQUE (source_system, object_key, file_version),
    CONSTRAINT ck_reconciliation_file_batch_id CHECK (id > 0),
    CONSTRAINT ck_reconciliation_file_batch_window CHECK (window_start < window_end),
    CONSTRAINT ck_reconciliation_file_batch_counts CHECK (file_version >= 0 AND schema_version > 0 AND record_count >= 0 AND byte_size >= 0),
    CONSTRAINT ck_reconciliation_file_batch_sha256 CHECK (sha256 ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ck_reconciliation_file_batch_text CHECK (length(btrim(source_system)) > 0 AND length(btrim(object_key)) > 0 AND length(btrim(record_type)) > 0),
    CONSTRAINT ck_reconciliation_file_batch_status CHECK (status IN ('CREATED','UPLOADING','DELIVERED','VALIDATING','VALIDATED','IMPORTING','IMPORTED','FAILED','QUARANTINED','COMPLETED')),
    -- 基础值域约束，不在数据库定义业务状态流转。
    CONSTRAINT ck_reconciliation_file_batch_completion CHECK (completed_at IS NULL OR completed_at >= created_at),
    CONSTRAINT ck_reconciliation_file_batch_actual_count CHECK (actual_record_count IS NULL OR actual_record_count >= 0),
    CONSTRAINT ck_reconciliation_file_batch_delivery_id CHECK (batch_id IS NULL OR batch_id > 0)
);
COMMENT ON TABLE reconciliation_file_batch IS '独立文件导入批次元数据；不绑定对账 Run；事务与状态策略由文件导入服务实现';
COMMENT ON COLUMN reconciliation_file_batch.id IS '应用分配的文件主键';
COMMENT ON COLUMN reconciliation_file_batch.batch_id IS '所属多文件交付批次的可空逻辑关联，不建立物理外键';
COMMENT ON COLUMN reconciliation_file_batch.source_system IS '来源系统';
COMMENT ON COLUMN reconciliation_file_batch.object_key IS '对象存储相对路径';
COMMENT ON COLUMN reconciliation_file_batch.record_type IS '文件记录类型';
COMMENT ON COLUMN reconciliation_file_batch.file_version IS '文件交付版本，不代表来源业务版本';
COMMENT ON COLUMN reconciliation_file_batch.schema_version IS 'CSV 格式版本';
COMMENT ON COLUMN reconciliation_file_batch.business_date IS '来源约定的业务日期，未知时为空';
COMMENT ON COLUMN reconciliation_file_batch.window_start IS '业务窗口起点，包含';
COMMENT ON COLUMN reconciliation_file_batch.window_end IS '业务窗口终点，不包含';
COMMENT ON COLUMN reconciliation_file_batch.cutoff_at IS '来源采集可见截止时间，未知时为空';
COMMENT ON COLUMN reconciliation_file_batch.sha256 IS '文件原始字节的 SHA-256 小写十六进制摘要';
COMMENT ON COLUMN reconciliation_file_batch.byte_size IS '文件字节数';
COMMENT ON COLUMN reconciliation_file_batch.record_count IS '预期数据行数，不含 CSV 表头';
COMMENT ON COLUMN reconciliation_file_batch.actual_record_count IS '实际处理数据行数，未处理时可空';
COMMENT ON COLUMN reconciliation_file_batch.status IS 'CREATED、UPLOADING、DELIVERED、VALIDATING、VALIDATED、IMPORTING、IMPORTED、FAILED、QUARANTINED；保留历史 COMPLETED';
COMMENT ON COLUMN reconciliation_file_batch.failure_reason IS '失败说明，可空';
COMMENT ON COLUMN reconciliation_file_batch.completed_at IS '导入结束时间，可空';
COMMENT ON COLUMN reconciliation_file_batch.created_at IS '元数据创建时间';
COMMENT ON COLUMN reconciliation_file_batch.updated_at IS '元数据最近更新时间';
CREATE INDEX idx_reconciliation_file_batch_status ON reconciliation_file_batch (status, created_at);
COMMENT ON INDEX idx_reconciliation_file_batch_status IS '按导入状态和创建时间查询文件批次';
CREATE INDEX idx_reconciliation_file_batch_delivery ON reconciliation_file_batch (batch_id, id);
COMMENT ON INDEX idx_reconciliation_file_batch_delivery IS '按交付批次批量读取文件描述';
CREATE INDEX idx_reconciliation_file_batch_source_date ON reconciliation_file_batch (source_system, business_date, id);
COMMENT ON INDEX idx_reconciliation_file_batch_source_date IS '按来源和业务日期读取文件页';
CREATE INDEX idx_reconciliation_file_batch_status_page ON reconciliation_file_batch (status, id);
COMMENT ON INDEX idx_reconciliation_file_batch_status_page IS '按文件状态进行稳定 ID 分页，保留原状态时间索引';
