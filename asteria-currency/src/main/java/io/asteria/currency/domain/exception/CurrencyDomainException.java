package io.asteria.currency.domain.exception;

import io.asteria.common.domain.exception.BusinessException;
import io.asteria.currency.domain.error.CurrencyErrorCode;

/**
 * 保留模块业务错误及校验行为的领域异常。
 */
public class CurrencyDomainException extends BusinessException {

    /** 业务错误码 */
    private final CurrencyErrorCode errorCode;

    /**
     * 构造模块异常，保留原有错误消息与原因。
     *
     * @param errorCode 模块业务错误码
     */
    public CurrencyDomainException(CurrencyErrorCode errorCode) {
        this(requireErrorCode(errorCode), requireErrorCode(errorCode).defaultMessage(), null);
    }

    /**
     * 构造模块异常，保留原有错误消息与原因。
     *
     * @param errorCode 模块业务错误码
     * @param detailMessage 详细错误消息
     */
    public CurrencyDomainException(CurrencyErrorCode errorCode, String detailMessage) {
        this(requireErrorCode(errorCode), detailMessage, null);
    }

    /**
     * 构造模块异常，保留原有错误消息与原因。
     *
     * @param errorCode 模块业务错误码
     * @param cause 原始失败原因
     */
    public CurrencyDomainException(CurrencyErrorCode errorCode, Throwable cause) {
        this(requireErrorCode(errorCode), requireErrorCode(errorCode).defaultMessage(), cause);
    }

    /**
     * 构造模块异常，保留原有错误消息与原因。
     *
     * @param errorCode 模块业务错误码
     * @param message 错误消息
     * @param cause 原始失败原因
     */
    private CurrencyDomainException(CurrencyErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
        this.errorCode = errorCode;
    }

    /**
     * 返回模块业务错误码。
     *
     * @return 业务错误码
     */
    public CurrencyErrorCode errorCode() {
        return errorCode;
    }

    /**
     * 校验并返回模块错误码，沿用原有空值处理。
     *
     * @param errorCode 模块业务错误码
     * @return 业务错误码
     */
    private static CurrencyErrorCode requireErrorCode(CurrencyErrorCode errorCode) {
        if (errorCode == null) {
            throw new CurrencyDomainException(CurrencyErrorCode.NULL_ARGUMENT, "errorCode must not be null");
        }
        return errorCode;
    }
}
