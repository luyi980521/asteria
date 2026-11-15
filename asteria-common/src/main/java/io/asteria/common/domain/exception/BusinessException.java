package io.asteria.common.domain.exception;

import io.asteria.common.domain.error.ErrorCode;

/**
 * 统一承载业务错误码，保留模块异常的消息与异常链。
 */
public class BusinessException extends RuntimeException {
    /** 业务错误码 */
    private final ErrorCode errorCode;

    /**
     * 按模块指定的消息构造异常，允许调用方随后通过 initCause 设置原因。
     *
     * @param errorCode 业务错误码
     * @param message 模块指定的错误消息
     */
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * 按模块指定的消息与原因构造异常。
     *
     * @param errorCode 业务错误码
     * @param message 模块指定的错误消息
     * @param cause 原始失败原因，可空
     */
    public BusinessException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    /**
     * 返回模块提供的业务错误码。
     *
     * @return 业务错误码
     */
    public ErrorCode errorCode() {
        return errorCode;
    }
}
