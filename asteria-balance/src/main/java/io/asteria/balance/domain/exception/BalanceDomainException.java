package io.asteria.balance.domain.exception;

import io.asteria.balance.domain.error.BalanceErrorCode;

public class BalanceDomainException extends RuntimeException {
    /** 领域错误码 */
    private final BalanceErrorCode errorCode;

    public BalanceDomainException(BalanceErrorCode errorCode) {
        super(requireErrorCode(errorCode).defaultMessage());
        this.errorCode = errorCode;
    }

    public BalanceErrorCode errorCode() {
        return errorCode;
    }

    private static BalanceErrorCode requireErrorCode(BalanceErrorCode errorCode) {
        if (errorCode == null) {
            throw new BalanceDomainException(BalanceErrorCode.BALANCE_ERROR_CODE_REQUIRED);
        }
        return errorCode;
    }
}
