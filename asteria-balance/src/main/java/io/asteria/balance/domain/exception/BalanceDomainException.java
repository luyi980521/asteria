package io.asteria.balance.domain.exception;

import io.asteria.common.domain.exception.BusinessException;
import io.asteria.balance.domain.error.BalanceErrorCode;

/**
 * 保留模块业务错误及校验行为的领域异常。
 */
public class BalanceDomainException extends BusinessException {
    /** 领域错误码 */
    private final BalanceErrorCode errorCode;

    /**
     * 构造模块异常，保留原有错误消息与原因。
     *
     * @param errorCode 模块业务错误码
     */
    public BalanceDomainException(BalanceErrorCode errorCode) {
        super(requireErrorCode(errorCode), errorCode.defaultMessage());
        this.errorCode = errorCode;
    }

    /**
     * 返回模块业务错误码。
     *
     * @return 业务错误码
     */
    public BalanceErrorCode errorCode() {
        return errorCode;
    }

    /**
     * 校验并返回模块错误码，沿用原有空值处理。
     *
     * @param errorCode 模块业务错误码
     * @return 业务错误码
     */
    private static BalanceErrorCode requireErrorCode(BalanceErrorCode errorCode) {
        if (errorCode == null) {
            throw new BalanceDomainException(BalanceErrorCode.BALANCE_ERROR_CODE_REQUIRED);
        }
        return errorCode;
    }
}
