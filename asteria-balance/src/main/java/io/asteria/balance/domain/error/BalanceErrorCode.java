package io.asteria.balance.domain.error;

import io.asteria.common.domain.error.ErrorCode;

/**
 * 模块业务错误码与默认消息。
 */
public enum BalanceErrorCode implements ErrorCode {
    /** Balance error code must not be null */
    BALANCE_ERROR_CODE_REQUIRED("BALANCE_0000", "Balance error code must not be null"),
    /** Amount must be greater than zero */
    INVALID_AMOUNT("BALANCE_0001", "Amount must be greater than zero"),
    /** Currencies do not match */
    CURRENCY_MISMATCH("BALANCE_0002", "Currencies do not match"),
    /** Insufficient available balance */
    INSUFFICIENT_AVAILABLE_BALANCE("BALANCE_0003", "Insufficient available balance"),
    /** Insufficient reserved balance */
    INSUFFICIENT_RESERVED_BALANCE("BALANCE_0004", "Insufficient reserved balance"),
    /** Invalid balance state */
    INVALID_BALANCE_STATE("BALANCE_0005", "Invalid balance state"),
    /** Invalid balance parameters */
    INVALID_PARAMS("BALANCE_0006", "Invalid balance parameters"),
    /** Invalid reservation state */
    INVALID_RESERVATION_STATE("BALANCE_0007", "Invalid reservation state"),
    /** Balance was concurrently modified */
    BALANCE_CONCURRENT_MODIFICATION("BALANCE_0008", "Balance was concurrently modified"),
    /** Reservation request is identical to the existing reservation */
    RESERVATION_REQUEST_IDENTICAL("BALANCE_0009", "Reservation request is identical to the existing reservation"),
    /** Reservation request conflicts with the existing reservation */
    RESERVATION_REQUEST_CONFLICT("BALANCE_0010", "Reservation request conflicts with the existing reservation"),
    /** Reservation status is incorrect */
    RESERVATION_STATUS_INCORRECT("BALANCE_0011", "Reservation status is incorrect"),
    /** Balance already exists */
    BALANCE_ALREADY_EXISTS("BALANCE_0012", "Balance already exists"),

    ;

    /** 错误码 */
    private final String code;
    /** 默认错误消息 */
    private final String defaultMessage;

    /**
     * 保存业务错误码与默认消息。
     *
     * @param code 业务错误码
     * @param defaultMessage 默认错误消息
     */
    BalanceErrorCode(String code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    /**
     * 返回稳定的业务错误码。
     *
     * @return 业务错误码
     */
    public String code() {
        return code;
    }

    /**
     * 返回默认错误消息。
     *
     * @return 默认错误消息
     */
    public String defaultMessage() {
        return defaultMessage;
    }
}
