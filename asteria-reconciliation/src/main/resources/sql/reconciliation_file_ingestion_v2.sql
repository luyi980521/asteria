-- 依次执行已有 reconciliation_file_batch.sql 和本增量脚本；不修改六张核心表，不自动执行。
-- 保留历史单文件身份与状态；新增交付头和逻辑关联，不覆盖历史来源数据。
CREATE TABLE reconciliation_delivery_batch (
    batch_id BIGINT PRIMARY KEY,
    manifest_object_key TEXT NOT NULL,
    source_system VARCHAR(64) NOT NULL,
    business_date DATE NOT NULL,
    window_start TIMESTAMPTZ NOT NULL,
    window_end TIMESTAMPTZ NOT NULL,
    cutoff_at TIMESTAMPTZ NOT NULL,
    file_version BIGINT NOT NULL,
    schema_version INTEGER NOT NULL,
    expected_record_count BIGINT NOT NULL,
    actual_record_count BIGINT,
    status VARCHAR(32) NOT NULL,
    failure_reason TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    -- 一个来源的同一 Manifest 路径与版本只登记一次。
    CONSTRAINT uk_reconciliation_delivery_batch_delivery UNIQUE (source_system, manifest_object_key, file_version),
    CONSTRAINT ck_reconciliation_delivery_batch_id CHECK (batch_id > 0),
    CONSTRAINT ck_reconciliation_delivery_batch_text CHECK (length(btrim(source_system)) > 0 AND length(btrim(manifest_object_key)) > 0),
    CONSTRAINT ck_reconciliation_delivery_batch_window CHECK (window_start < window_end),
    CONSTRAINT ck_reconciliation_delivery_batch_counts CHECK (file_version >= 0 AND schema_version > 0 AND expected_record_count >= 0 AND (actual_record_count IS NULL OR actual_record_count >= 0)),
    CONSTRAINT ck_reconciliation_delivery_batch_status CHECK (status IN ('CREATED','UPLOADING','DELIVERED','VALIDATING','VALIDATED','IMPORTING','IMPORTED','FAILED','QUARANTINED')),
    CONSTRAINT ck_reconciliation_delivery_batch_time CHECK (updated_at >= created_at AND (completed_at IS NULL OR completed_at >= created_at))
);
COMMENT ON TABLE reconciliation_delivery_batch IS '多文件交付批次头，文件描述保存在 reconciliation_file_batch，状态转换由调用方实现';
COMMENT ON COLUMN reconciliation_delivery_batch.batch_id IS '应用 DistributedIdGenerator 分配的正数批次主键';
COMMENT ON COLUMN reconciliation_delivery_batch.manifest_object_key IS '独立 JSON Manifest 的对象存储键';
COMMENT ON COLUMN reconciliation_delivery_batch.source_system IS '交付来源系统';
COMMENT ON COLUMN reconciliation_delivery_batch.business_date IS '来源系统约定的业务日期，不自动按 UTC 推导';
COMMENT ON COLUMN reconciliation_delivery_batch.window_start IS '包含的业务时间窗口起点';
COMMENT ON COLUMN reconciliation_delivery_batch.window_end IS '不包含的业务时间窗口终点';
COMMENT ON COLUMN reconciliation_delivery_batch.cutoff_at IS '来源采集可见截止时间';
COMMENT ON COLUMN reconciliation_delivery_batch.file_version IS '文件交付版本，不等于来源业务版本';
COMMENT ON COLUMN reconciliation_delivery_batch.schema_version IS 'Manifest 格式版本';
COMMENT ON COLUMN reconciliation_delivery_batch.expected_record_count IS '全部 CSV 预期数据行数之和，不含表头';
COMMENT ON COLUMN reconciliation_delivery_batch.actual_record_count IS '实际处理行数，未处理时可空';
COMMENT ON COLUMN reconciliation_delivery_batch.status IS '文件交付与导入状态，仅限定值域，不实现业务状态转换';
COMMENT ON COLUMN reconciliation_delivery_batch.failure_reason IS '失败或隔离说明，TEXT 原样保存';
COMMENT ON COLUMN reconciliation_delivery_batch.created_at IS '批次登记时间';
COMMENT ON COLUMN reconciliation_delivery_batch.updated_at IS '元数据最近更新时间，也用于条件更新';
COMMENT ON COLUMN reconciliation_delivery_batch.completed_at IS '处理结束时间，可空';

CREATE INDEX idx_reconciliation_delivery_batch_source_date ON reconciliation_delivery_batch (source_system, business_date, batch_id);
COMMENT ON INDEX idx_reconciliation_delivery_batch_source_date IS '按来源与业务日期进行稳定 ID 分页';
CREATE INDEX idx_reconciliation_delivery_batch_status ON reconciliation_delivery_batch (status, batch_id);
COMMENT ON INDEX idx_reconciliation_delivery_batch_status IS '按批次状态进行稳定 ID 分页';

ALTER TABLE reconciliation_file_batch
    ADD COLUMN batch_id BIGINT,
    ADD COLUMN business_date DATE,
    ADD COLUMN cutoff_at TIMESTAMPTZ,
    ADD COLUMN actual_record_count BIGINT,
    ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP;
COMMENT ON COLUMN reconciliation_file_batch.batch_id IS '所属多文件交付批次的逻辑关联，历史单文件记录可空，不建立物理外键';
COMMENT ON COLUMN reconciliation_file_batch.business_date IS '来源约定的业务日期，历史记录未知时保持空值';
COMMENT ON COLUMN reconciliation_file_batch.cutoff_at IS '来源采集可见截止时间，历史记录可空';
COMMENT ON COLUMN reconciliation_file_batch.actual_record_count IS '实际处理数据行数，未处理时可空';
COMMENT ON COLUMN reconciliation_file_batch.updated_at IS '元数据更新时间；历史行初值为本次迁移时间';
COMMENT ON COLUMN reconciliation_file_batch.record_count IS '预期数据行数，不含 CSV 表头';
COMMENT ON COLUMN reconciliation_file_batch.status IS 'CREATED、UPLOADING、DELIVERED、VALIDATING、VALIDATED、IMPORTING、IMPORTED、FAILED、QUARANTINED；保留历史 COMPLETED';

ALTER TABLE reconciliation_file_batch DROP CONSTRAINT ck_reconciliation_file_batch_status;
ALTER TABLE reconciliation_file_batch DROP CONSTRAINT ck_reconciliation_file_batch_completion;
ALTER TABLE reconciliation_file_batch DROP CONSTRAINT ck_reconciliation_file_batch_failure;
ALTER TABLE reconciliation_file_batch
    ADD CONSTRAINT ck_reconciliation_file_batch_status CHECK (status IN ('CREATED','UPLOADING','DELIVERED','VALIDATING','VALIDATED','IMPORTING','IMPORTED','FAILED','QUARANTINED','COMPLETED')),
    -- 只检查基本数据值域，不在数据库编码业务状态流转与失败重试策略。
    ADD CONSTRAINT ck_reconciliation_file_batch_completion CHECK (completed_at IS NULL OR completed_at >= created_at),
    ADD CONSTRAINT ck_reconciliation_file_batch_actual_count CHECK (actual_record_count IS NULL OR actual_record_count >= 0),
    ADD CONSTRAINT ck_reconciliation_file_batch_delivery_id CHECK (batch_id IS NULL OR batch_id > 0);
CREATE INDEX idx_reconciliation_file_batch_delivery ON reconciliation_file_batch (batch_id, file_id);
COMMENT ON INDEX idx_reconciliation_file_batch_delivery IS '按交付批次批量读取文件描述';
CREATE INDEX idx_reconciliation_file_batch_source_date ON reconciliation_file_batch (source_system, business_date, file_id);
COMMENT ON INDEX idx_reconciliation_file_batch_source_date IS '按来源和业务日期读取文件页';
CREATE INDEX idx_reconciliation_file_batch_status_page ON reconciliation_file_batch (status, file_id);
COMMENT ON INDEX idx_reconciliation_file_batch_status_page IS '按文件状态进行稳定 ID 分页，保留原状态时间索引';
