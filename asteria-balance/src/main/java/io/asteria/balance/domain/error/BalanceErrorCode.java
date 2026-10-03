package io.asteria.balance.domain.error;

public enum BalanceErrorCode {
    BALANCE_ERROR_CODE_REQUIRED("BALANCE_0000", "Balance error code must not be null"),
    INVALID_AMOUNT("BALANCE_0001", "Amount must be greater than zero"),
    CURRENCY_MISMATCH("BALANCE_0002", "Currencies do not match"),
    INSUFFICIENT_AVAILABLE_BALANCE("BALANCE_0003", "Insufficient available balance"),
    INSUFFICIENT_RESERVED_BALANCE("BALANCE_0004", "Insufficient reserved balance"),
    INVALID_BALANCE_STATE("BALANCE_0005", "Invalid balance state"),
    INVALID_PARAMS("BALANCE_0006", "Invalid balance parameters"),
    INVALID_RESERVATION_STATE("BALANCE_0007", "Invalid reservation state"),
    BALANCE_CONCURRENT_MODIFICATION("BALANCE_0008", "Balance was concurrently modified");

    /** 错误码 */
    private final String code;
    /** 默认错误消息 */
    private final String defaultMessage;

    BalanceErrorCode(String code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    public String code() {
        return code;
    }

    public String defaultMessage() {
        return defaultMessage;
    }
}
