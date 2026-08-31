package io.asteria.settlement.domain.exception;

import io.asteria.settlement.domain.error.SettlementErrorCode;

/**
 * Settlement 领域异常
 */
public class SettlementDomainException extends RuntimeException {

    private final SettlementErrorCode errorCode;

    public SettlementDomainException(SettlementErrorCode errorCode) {
        super(errorCode.name());
        this.errorCode = errorCode;
    }

    public SettlementErrorCode getErrorCode() {
        return errorCode;
    }
}