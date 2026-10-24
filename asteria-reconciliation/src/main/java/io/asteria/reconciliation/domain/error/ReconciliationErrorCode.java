package io.asteria.reconciliation.domain.error;

/**
 * 对账领域错误码；数据库唯一性冲突仍由持久化层报告。
 */
public enum ReconciliationErrorCode {
    /** 参数、时间或持久化数据违反领域约束。 */
    INVALID_PARAMS("RECONCILIATION_0001", "Reconciliation parameters violate domain constraints"),
    /** 当前状态不允许请求的转换。 */
    INVALID_STATE("RECONCILIATION_0002", "Reconciliation state transition is not allowed"),
    /** 序列化快照或来源 ID 列表格式不合法。 */
    INVALID_JSON("RECONCILIATION_0003", "Reconciliation serialized data is invalid"),
    /** 版本冲突或更新目标不存在。 */
    CONCURRENT_MODIFICATION("RECONCILIATION_0004", "Reconciliation record was modified or does not exist");

    /** 稳定的模块错误码。 */
    private final String code;
    /** 默认诊断信息。 */
    private final String defaultMessage;

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
