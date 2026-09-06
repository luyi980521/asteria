package io.asteria.currency.domain.error;

public enum CurrencyErrorCode {
    NULL_ARGUMENT("CURRENCY_0001", "Argument must not be null"),
    CURRENCY_CODE_REQUIRED("CURRENCY_0002", "Currency code must not be null"),
    DISPLAY_NAME_REQUIRED("CURRENCY_0003", "Currency display name must not be blank"),
    INVALID_MINOR_UNIT("CURRENCY_0004", "Currency minor unit must be between zero and four"),
    INVALID_NUMERIC_CODE("CURRENCY_0005", "Currency numeric code must contain three digits"),
    CREATED_AT_REQUIRED("CURRENCY_0006", "Currency created time must not be null"),
    UPDATED_AT_REQUIRED("CURRENCY_0007", "Currency updated time must not be null"),
    CURRENCY_NOT_FOUND("CURRENCY_0008", "Currency definition does not exist");

    private final String code;
    private final String defaultMessage;

    CurrencyErrorCode(String code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    public String code() { return code; }

    public String defaultMessage() { return defaultMessage; }
}
