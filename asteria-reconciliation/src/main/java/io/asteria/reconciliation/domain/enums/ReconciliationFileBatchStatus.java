package io.asteria.reconciliation.domain.enums;

/**
 * 文件交付与导入状态；具体转换、重试及恢复由用户实现。
 */
public enum ReconciliationFileBatchStatus {

    /** 已登记 */
    CREATED,

    /** 文件上传中 */
    UPLOADING,

    /** 已交付 */
    DELIVERED,

    /** 文件验证中 */
    VALIDATING,

    /** 验证完成 */
    VALIDATED,

    /** 来源记录导入中 */
    IMPORTING,

    /** 来源记录导入完成 */
    IMPORTED,

    /** 处理失败 */
    FAILED,

    /** 文件已隔离 */
    QUARANTINED,

    /** 兼容旧单文件表的完成状态，新交付使用 IMPORTED */
    @Deprecated
    COMPLETED
}
