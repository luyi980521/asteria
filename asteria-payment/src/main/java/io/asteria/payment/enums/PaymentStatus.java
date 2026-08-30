package io.asteria.payment.enums;

/**
 * 支付状态枚举定义
 * */
public enum PaymentStatus {

    /** 已创建 */
    CREATED,

    /** 授权处理中 */
    AUTHORIZING,

    /** 已授权 */
    AUTHORIZED,

    /** 捕获处理中 */
    CAPTURING,

    /** 已捕获 */
    CAPTURED,

    /** 支付失败 */
    FAILED,

    /** 已取消 */
    CANCELLED
}