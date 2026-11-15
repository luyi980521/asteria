-- 完整交付批次表定义，与 ReconciliationDeliveryBatchDO 的字段及定义顺序对应。
-- 本脚本只创建交付批次表，不修改其他表；不自动执行。
CREATE TABLE reconciliation_delivery_batch (
    id BIGINT PRIMARY KEY,
    source_system VARCHAR(64) NOT NULL,
    manifest_object_key TEXT NOT NULL,
    delivery_version BIGINT NOT NULL,
    schema_version INTEGER NOT NULL,
    business_date DATE NOT NULL,
    window_start TIMESTAMPTZ NOT NULL,
    window_end TIMESTAMPTZ NOT NULL,
    cutoff_at TIMESTAMPTZ NOT NULL,
    expected_record_count BIGINT NOT NULL,
    actual_record_count BIGINT,
    status VARCHAR(32) NOT NULL,
    failure_reason TEXT,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    -- 一个来源的同一 Manifest 路径与版本只登记一次。
    CONSTRAINT uk_reconciliation_delivery_batch_delivery UNIQUE (source_system, manifest_object_key, delivery_version),
    CONSTRAINT ck_reconciliation_delivery_batch_id CHECK (id > 0),
    CONSTRAINT ck_reconciliation_delivery_batch_text CHECK (length(btrim(source_system)) > 0 AND length(btrim(manifest_object_key)) > 0),
    CONSTRAINT ck_reconciliation_delivery_batch_window CHECK (window_start < window_end),
    CONSTRAINT ck_reconciliation_delivery_batch_counts CHECK (delivery_version >= 0 AND schema_version > 0 AND expected_record_count >= 0 AND (actual_record_count IS NULL OR actual_record_count >= 0)),
    CONSTRAINT ck_reconciliation_delivery_batch_status CHECK (status IN ('CREATED','UPLOADING','DELIVERED','VALIDATING','VALIDATED','IMPORTING','IMPORTED','FAILED','QUARANTINED')),
    CONSTRAINT ck_reconciliation_delivery_batch_time CHECK (updated_at >= created_at AND (completed_at IS NULL OR completed_at >= created_at))
);
COMMENT ON TABLE reconciliation_delivery_batch IS '多文件交付批次头，文件描述保存在 reconciliation_file_batch，状态转换由调用方实现';
COMMENT ON COLUMN reconciliation_delivery_batch.id IS '应用 DistributedIdGenerator 分配的正数批次主键';
COMMENT ON COLUMN reconciliation_delivery_batch.source_system IS '交付来源系统';
COMMENT ON COLUMN reconciliation_delivery_batch.manifest_object_key IS '独立 JSON Manifest 的对象存储键';
COMMENT ON COLUMN reconciliation_delivery_batch.delivery_version IS '文件交付版本，不等于来源业务版本';
COMMENT ON COLUMN reconciliation_delivery_batch.schema_version IS 'Manifest 格式版本';
COMMENT ON COLUMN reconciliation_delivery_batch.business_date IS '来源系统约定的业务日期，不自动按 UTC 推导';
COMMENT ON COLUMN reconciliation_delivery_batch.window_start IS '包含的业务时间窗口起点';
COMMENT ON COLUMN reconciliation_delivery_batch.window_end IS '不包含的业务时间窗口终点';
COMMENT ON COLUMN reconciliation_delivery_batch.cutoff_at IS '来源采集可见截止时间';
COMMENT ON COLUMN reconciliation_delivery_batch.expected_record_count IS '全部 CSV 预期数据行数之和，不含表头';
COMMENT ON COLUMN reconciliation_delivery_batch.actual_record_count IS '实际处理行数，未处理时可空';
COMMENT ON COLUMN reconciliation_delivery_batch.status IS '文件交付与导入状态，仅限定值域，不实现业务状态转换';
COMMENT ON COLUMN reconciliation_delivery_batch.failure_reason IS '失败或隔离说明，TEXT 原样保存';
COMMENT ON COLUMN reconciliation_delivery_batch.completed_at IS '处理结束时间，可空';
COMMENT ON COLUMN reconciliation_delivery_batch.created_at IS '批次登记时间';
COMMENT ON COLUMN reconciliation_delivery_batch.updated_at IS '元数据最近更新时间，也用于条件更新';

CREATE INDEX idx_reconciliation_delivery_batch_source_date ON reconciliation_delivery_batch (source_system, business_date, id);
COMMENT ON INDEX idx_reconciliation_delivery_batch_source_date IS '按来源与业务日期进行稳定 ID 分页';
CREATE INDEX idx_reconciliation_delivery_batch_status ON reconciliation_delivery_batch (status, id);
COMMENT ON INDEX idx_reconciliation_delivery_batch_status IS '按批次状态进行稳定 ID 分页';

