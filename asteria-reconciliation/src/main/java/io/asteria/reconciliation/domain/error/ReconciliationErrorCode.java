package io.asteria.reconciliation.domain.error;

import io.asteria.common.domain.error.ErrorCode;

/**
 * 对账领域错误码；数据库唯一性冲突仍由持久化层报告。
 */
public enum ReconciliationErrorCode implements ErrorCode {
    /** 参数、时间或持久化数据违反领域约束。 */
    INVALID_PARAMS("RECONCILIATION_0001", "Reconciliation parameters violate domain constraints"),
    /** 当前状态不允许请求的转换。 */
    INVALID_STATE("RECONCILIATION_0002", "Reconciliation state transition is not allowed"),
    /** 序列化快照或来源 ID 列表格式不合法。 */
    INVALID_JSON("RECONCILIATION_0003", "Reconciliation serialized data is invalid"),
    /** 版本冲突或更新目标不存在。 */
    CONCURRENT_MODIFICATION("RECONCILIATION_0004", "Reconciliation record was modified or does not exist"),

    /** 清单结构、文件描述或清单内部关系不合法。 */
    INVALID_MANIFEST("RECONCILIATION_0005", "Reconciliation manifest is invalid"),
    /** 清单交付标识与当前批次不一致。 */
    MANIFEST_DELIVERY_MISMATCH("RECONCILIATION_0006", "Manifest delivery ID differs from delivery batch"),
    /** 文件批次分页包含空记录、重复记录或未递增标识。 */
    INVALID_FILE_BATCH_PAGE("RECONCILIATION_0007", "File batch IDs are not strictly increasing"),
    /** 登记文件未出现在清单中或对象键重复。 */
    UNEXPECTED_FILE_BATCH("RECONCILIATION_0008", "Unexpected or duplicate file batch object key"),
    /** 登记文件身份、批次关联或文件描述与清单不一致。 */
    FILE_BATCH_METADATA_MISMATCH("RECONCILIATION_0009", "File batch metadata differs from manifest"),
    /** 登记文件覆盖范围、文件数量或记录总数与清单不一致。 */
    FILE_BATCH_TOTAL_MISMATCH("RECONCILIATION_0010", "File batches do not cover manifest files or record count"),
    /** 累加文件预期记录数时发生整数溢出。 */
    FILE_RECORD_COUNT_OVERFLOW("RECONCILIATION_0011", "File batch record count overflow"),

    ;
    /** 稳定的模块错误码。 */
    private final String code;
    /** 默认诊断信息。 */
    private final String defaultMessage;

    /**
     * 保存业务错误码与默认消息。
     *
     * @param code 业务错误码
     * @param defaultMessage 默认错误消息
     */
    ReconciliationErrorCode(String code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    /**
     * 返回可供调用方识别的错误码。
     */
    public String code() { return code; }

    /**
     * 返回默认错误说明。
     */
    public String defaultMessage() { return defaultMessage; }
}
