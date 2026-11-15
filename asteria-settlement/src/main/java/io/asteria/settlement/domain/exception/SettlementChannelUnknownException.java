package io.asteria.settlement.domain.exception;

import io.asteria.common.domain.exception.BusinessException;
import io.asteria.settlement.domain.error.SettlementErrorCode;

/**
 * Settlement 对接外部的未知异常
 */
public class SettlementChannelUnknownException extends BusinessException {

    /** 业务错误码 */
    private final SettlementErrorCode errorCode;

    /**
     * 构造模块异常，保留原有错误消息与原因。
     *
     * @param errorCode 模块业务错误码
     */
    public SettlementChannelUnknownException(SettlementErrorCode errorCode) {
        super(errorCode, errorCode.name());
        this.errorCode = errorCode;
    }

    /**
     * 返回模块业务错误码。
     *
     * @return 业务错误码
     */
    public SettlementErrorCode getErrorCode() {
        return errorCode;
    }
}