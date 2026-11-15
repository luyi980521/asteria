package io.asteria.ledger.domain.exception;

import io.asteria.common.domain.exception.BusinessException;
import io.asteria.ledger.domain.error.LedgerErrorCode;

/**
 * 保留模块业务错误及校验行为的领域异常。
 */
public class LedgerDomainException extends BusinessException {

    /** 业务错误码 */
    private final LedgerErrorCode errorCode;

    /**
     * 构造模块异常，保留原有错误消息与原因。
     *
     * @param errorCode 模块业务错误码
     */
    public LedgerDomainException(LedgerErrorCode errorCode) {
        this(requireErrorCode(errorCode), requireErrorCode(errorCode).defaultMessage(), null);
    }

    /**
     * 构造模块异常，保留原有错误消息与原因。
     *
     * @param errorCode 模块业务错误码
     * @param detailMessage 详细错误消息
     */
    public LedgerDomainException(LedgerErrorCode errorCode, String detailMessage) {
        this(requireErrorCode(errorCode), detailMessage, null);
    }

    /**
     * 构造模块异常，保留原有错误消息与原因。
     *
     * @param errorCode 模块业务错误码
     * @param cause 原始失败原因
     */
    public LedgerDomainException(LedgerErrorCode errorCode, Throwable cause) {
        this(requireErrorCode(errorCode), requireErrorCode(errorCode).defaultMessage(), cause);
    }

    /**
     * 构造模块异常，保留原有错误消息与原因。
     *
     * @param errorCode 模块业务错误码
     * @param message 错误消息
     * @param cause 原始失败原因
     */
    private LedgerDomainException(LedgerErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
        this.errorCode = errorCode;
    }

    /**
     * 返回模块业务错误码。
     *
     * @return 业务错误码
     */
    public LedgerErrorCode errorCode() {
        return errorCode;
    }

    /**
     * 校验并返回模块错误码，沿用原有空值处理。
     *
     * @param errorCode 模块业务错误码
     * @return 业务错误码
     */
    private static LedgerErrorCode requireErrorCode(LedgerErrorCode errorCode) {
        if (errorCode == null) {
            throw new LedgerDomainException(LedgerErrorCode.NULL_ARGUMENT, "errorCode must not be null");
        }
        return errorCode;
    }
}
