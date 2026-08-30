package io.asteria.common.domain.error;

public enum CommonErrorCode {
    /*********************** 通用错误 ***********************/
    NULL_ARGUMENT("COMMON_0001", "Argument must not be null"),

    /*********************** Money 错误 ***********************/
    MONEY_AMOUNT_REQUIRED("COMMON_0002", "Money amount must not be null"),
    MONEY_AMOUNT_MUST_BE_POSITIVE("COMMON_0003", "Money amount must be greater than zero"),
    MONEY_AMOUNT_SCALE_INVALID("COMMON_0004", "Money amount has too many fraction digits"),
    MONEY_CURRENCY_REQUIRED("COMMON_0005", "Money currency must not be null"),
    MONEY_CURRENCY_MISMATCH("COMMON_0006", "Currencies do not match"),
    MONEY_OTHER_REQUIRED("COMMON_0007", "Other money must not be null"),

    ;

    private final String code;
    private final String defaultMessage;

    CommonErrorCode(String code, String defaultMessage) {
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
