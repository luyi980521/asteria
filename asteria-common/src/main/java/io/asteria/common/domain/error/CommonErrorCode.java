package io.asteria.common.domain.error;

/**
 * 模块业务错误码与默认消息。
 */
public enum CommonErrorCode implements ErrorCode {
    /** ********************* 通用错误 ********************** */
    NULL_ARGUMENT("COMMON_0001", "Argument must not be null"),

    /** ********************* Money 错误 ********************** */
    MONEY_AMOUNT_REQUIRED("COMMON_0002", "Money amount must not be null"),
    /** Money amount must be greater than zero */
    MONEY_AMOUNT_MUST_BE_POSITIVE("COMMON_0003", "Money amount must be greater than zero"),
    /** Money amount has too many fraction digits */
    MONEY_AMOUNT_SCALE_INVALID("COMMON_0004", "Money amount has too many fraction digits"),
    /** Money currency must not be null */
    MONEY_CURRENCY_REQUIRED("COMMON_0005", "Money currency must not be null"),
    /** Currencies do not match */
    MONEY_CURRENCY_MISMATCH("COMMON_0006", "Currencies do not match"),
    /** Other money must not be null */
    MONEY_OTHER_REQUIRED("COMMON_0007", "Other money must not be null"),

    /** Currency code must not be blank */
    CURRENCY_CODE_REQUIRED("COMMON_0008", "Currency code must not be blank"),
    /** Currency code must contain three letters */
    INVALID_CURRENCY_CODE("COMMON_0009", "Currency code must contain three letters"),
    /** Money amount must not be negative */
    MONEY_AMOUNT_MUST_BE_NON_NEGATIVE("COMMON_0010", "Money amount must not be negative"),
    ;

    /** 业务错误码 */
    private final String code;
    /** 默认错误消息 */
    private final String defaultMessage;

    /**
     * 保存业务错误码与默认消息。
     *
     * @param code 业务错误码
     * @param defaultMessage 默认错误消息
     */
    CommonErrorCode(String code, String defaultMessage) {
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
