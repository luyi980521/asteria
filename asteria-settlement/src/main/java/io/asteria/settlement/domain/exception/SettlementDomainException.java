package io.asteria.settlement.domain.exception;

import io.asteria.common.domain.exception.BusinessException;
import io.asteria.settlement.domain.error.SettlementErrorCode;

/**
 * Settlement 领域异常
 */
public class SettlementDomainException extends BusinessException {

    /** 业务错误码 */
    private final SettlementErrorCode errorCode;

    /**
     * 构造模块异常，保留原有错误消息与原因。
     *
     * @param errorCode 模块业务错误码
     */
    public SettlementDomainException(SettlementErrorCode errorCode) {
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