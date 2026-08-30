package io.asteria.payment.domain.error;

public enum PaymentErrorCode {
    PAYMENT_ERROR_CODE_REQUIRED("PAYMENT_0000", "Payment error code must not be null"),
    PAYMENT_ID_MUST_BE_POSITIVE("PAYMENT_0001", "Payment id must be positive"),
    MERCHANT_ID_MUST_BE_POSITIVE("PAYMENT_0002", "Merchant id must be positive"),
    PAYMENT_AMOUNT_REQUIRED("PAYMENT_0003", "Payment amount must not be null"),
    PAYMENT_METHOD_REQUIRED("PAYMENT_0004", "Payment method must not be null"),
    PAYMENT_REFERENCE_REQUIRED("PAYMENT_0005", "Payment reference must not be null"),
    PAYMENT_CREATED_AT_REQUIRED("PAYMENT_0006", "Created time must not be null"),
    PAYMENT_AUTHORIZED_AT_REQUIRED("PAYMENT_0007", "Authorized time must not be null"),
    PAYMENT_CAPTURED_AT_REQUIRED("PAYMENT_0008", "Captured time must not be null"),
    PAYMENT_REFERENCE_TYPE_REQUIRED("PAYMENT_0009", "Payment reference type must not be blank"),
    PAYMENT_REFERENCE_ID_REQUIRED("PAYMENT_0010", "Payment reference id must not be blank"),
    PAYMENT_MUST_BE_CREATED_TO_AUTHORIZE("PAYMENT_0011", "Only created payment can start authorization"),
    PAYMENT_MUST_BE_AUTHORIZING_TO_AUTHORIZE("PAYMENT_0012", "Only authorizing payment can be authorized"),
    PAYMENT_MUST_BE_AUTHORIZED_TO_CAPTURE("PAYMENT_0013", "Only authorized payment can start capture"),
    PAYMENT_MUST_BE_CAPTURING_TO_CAPTURE("PAYMENT_0014", "Only capturing payment can be captured"),
    PAYMENT_CANNOT_FAIL_IN_CURRENT_STATUS("PAYMENT_0015", "Current payment cannot be marked as failed"),
    PAYMENT_CANNOT_CANCEL_IN_CURRENT_STATUS("PAYMENT_0016", "Current payment cannot be cancelled"),
    PAYMENT_NOT_FOUND("PAYMENT_0017", "Payment does not exist");

    private final String code;
    private final String defaultMessage;

    PaymentErrorCode(String code, String defaultMessage) {
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
