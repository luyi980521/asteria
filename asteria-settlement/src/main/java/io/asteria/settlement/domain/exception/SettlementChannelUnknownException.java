package io.asteria.settlement.domain.exception;

import io.asteria.settlement.domain.error.SettlementErrorCode;

/**
 * Settlement 对接外部的未知异常
 */
public class SettlementChannelUnknownException extends RuntimeException {

    private final SettlementErrorCode errorCode;

    public SettlementChannelUnknownException(SettlementErrorCode errorCode) {
        super(errorCode.name());
        this.errorCode = errorCode;
    }

    public SettlementErrorCode getErrorCode() {
        return errorCode;
    }
}