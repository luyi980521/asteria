package io.asteria.currency.domain.error;

import io.asteria.common.domain.error.ErrorCode;

/**
 * 模块业务错误码与默认消息。
 */
public enum CurrencyErrorCode implements ErrorCode {
    /** Argument must not be null */
    NULL_ARGUMENT("CURRENCY_0001", "Argument must not be null"),
    /** Currency code must not be null */
    CURRENCY_CODE_REQUIRED("CURRENCY_0002", "Currency code must not be null"),
    /** Currency display name must not be blank */
    DISPLAY_NAME_REQUIRED("CURRENCY_0003", "Currency display name must not be blank"),
    /** Currency minor unit must be between zero and four */
    INVALID_MINOR_UNIT("CURRENCY_0004", "Currency minor unit must be between zero and four"),
    /** Currency numeric code must contain three digits */
    INVALID_NUMERIC_CODE("CURRENCY_0005", "Currency numeric code must contain three digits"),
    /** Currency created time must not be null */
    CREATED_AT_REQUIRED("CURRENCY_0006", "Currency created time must not be null"),
    /** Currency updated time must not be null */
    UPDATED_AT_REQUIRED("CURRENCY_0007", "Currency updated time must not be null"),
    /** Currency definition does not exist */
    CURRENCY_NOT_FOUND("CURRENCY_0008", "Currency definition does not exist");

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
    CurrencyErrorCode(String code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    /**
     * 返回稳定的业务错误码。
     *
     * @return 业务错误码
     */
    public String code() { return code; }

    /**
     * 返回默认错误消息。
     *
     * @return 默认错误消息
     */
    public String defaultMessage() { return defaultMessage; }
}
