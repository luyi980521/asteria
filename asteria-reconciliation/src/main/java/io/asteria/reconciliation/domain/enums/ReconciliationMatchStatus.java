package io.asteria.reconciliation.domain.enums;

/**
 * 单个业务键的核对结果；PENDING 表示等待数据到齐，不能直接建差异 Case。
 */
public enum ReconciliationMatchStatus {
    /** 已匹配且无差异。 */
    MATCHED,
    /** 双方存在但属性或预期不符。 */
    MISMATCHED,
    /** 缺少预期来源记录。 */
    MISSING,
    /** 尚在等待期，暂不确定缺失。 */
    PENDING,
    /** 发现业务层重复记录。 */
    DUPLICATED
}
