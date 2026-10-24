package io.asteria.reconciliation.domain.exception;

import io.asteria.reconciliation.domain.error.ReconciliationErrorCode;

/**
 * 对账领域异常，携带稳定错误码及可选的校验失败原因。
 */
public class ReconciliationDomainException extends RuntimeException {
    /** 本次失败所属的领域错误。 */
    private final ReconciliationErrorCode errorCode;

    /**
     * 根据领域错误码构造异常。
     */
    public ReconciliationDomainException(ReconciliationErrorCode errorCode) {
        this(errorCode, null);
    }

    /**
     * 保留序列化等底层失败原因。
     */
    public ReconciliationDomainException(ReconciliationErrorCode errorCode, Throwable cause) {
        super((errorCode == null ? ReconciliationErrorCode.INVALID_PARAMS : errorCode).defaultMessage(), cause);
        this.errorCode = errorCode == null ? ReconciliationErrorCode.INVALID_PARAMS : errorCode;
    }

    /**
     * 返回领域错误码，沿用 Payment/Balance 的访问方式。
     */
    public ReconciliationErrorCode errorCode() { return errorCode; }
}
