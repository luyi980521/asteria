package io.asteria.payment.domain.error;

import io.asteria.common.domain.error.ErrorCode;

/**
 * 模块业务错误码与默认消息。
 */
public enum PaymentErrorCode implements ErrorCode {
    /** Payment error code must not be null */
    PAYMENT_ERROR_CODE_REQUIRED("PAYMENT_0000", "Payment error code must not be null"),
    /** Payment id must be positive */
    PAYMENT_ID_MUST_BE_POSITIVE("PAYMENT_0001", "Payment id must be positive"),
    /** Merchant id must be positive */
    MERCHANT_ID_MUST_BE_POSITIVE("PAYMENT_0002", "Merchant id must be positive"),
    /** Payment amount must not be null */
    PAYMENT_AMOUNT_REQUIRED("PAYMENT_0003", "Payment amount must not be null"),
    /** Payment method must not be null */
    PAYMENT_METHOD_REQUIRED("PAYMENT_0004", "Payment method must not be null"),
    /** Payment reference must not be null */
    PAYMENT_REFERENCE_REQUIRED("PAYMENT_0005", "Payment reference must not be null"),
    /** Created time must not be null */
    PAYMENT_CREATED_AT_REQUIRED("PAYMENT_0006", "Created time must not be null"),
    /** Authorized time must not be null */
    PAYMENT_AUTHORIZED_AT_REQUIRED("PAYMENT_0007", "Authorized time must not be null"),
    /** Captured time must not be null */
    PAYMENT_CAPTURED_AT_REQUIRED("PAYMENT_0008", "Captured time must not be null"),
    /** Payment reference type must not be blank */
    PAYMENT_REFERENCE_TYPE_REQUIRED("PAYMENT_0009", "Payment reference type must not be blank"),
    /** Payment reference id must not be blank */
    PAYMENT_REFERENCE_ID_REQUIRED("PAYMENT_0010", "Payment reference id must not be blank"),
    /** Only created payment can start authorization */
    PAYMENT_MUST_BE_CREATED_TO_AUTHORIZE("PAYMENT_0011", "Only created payment can start authorization"),
    /** Only authorizing payment can be authorized */
    PAYMENT_MUST_BE_AUTHORIZING_TO_AUTHORIZE("PAYMENT_0012", "Only authorizing payment can be authorized"),
    /** Only authorized payment can start capture */
    PAYMENT_MUST_BE_AUTHORIZED_TO_CAPTURE("PAYMENT_0013", "Only authorized payment can start capture"),
    /** Only capturing payment can be captured */
    PAYMENT_MUST_BE_CAPTURING_TO_CAPTURE("PAYMENT_0014", "Only capturing payment can be captured"),
    /** Current payment cannot be marked as failed */
    PAYMENT_CANNOT_FAIL_IN_CURRENT_STATUS("PAYMENT_0015", "Current payment cannot be marked as failed"),
    /** Current payment cannot be cancelled */
    PAYMENT_CANNOT_CANCEL_IN_CURRENT_STATUS("PAYMENT_0016", "Current payment cannot be cancelled"),
    /** Payment does not exist */
    PAYMENT_NOT_FOUND("PAYMENT_0017", "Payment does not exist"),
    /** Payment reference already exists */
    PAYMENT_REFERENCE_ALREADY_EXISTS("PAYMENT_0018", "Payment reference already exists"),
    /** Payment channel not found */
    PAYMENT_CHANNEL_NOT_FOUND("PAYMENT_0019", "Payment channel not found"),
    /** Payment capture fail to authorized */
    PAYMENT_CAPTURE_FAIL_TO_AUTHORIZED("PAYMENT_0020", "Payment capture fail to authorized"),
    /** Invalid payment status */
    INVALID_PAYMENT_STATUS("PAYMENT_0021", "Invalid payment status"),
    /** Authorization transaction id required */
    AUTHORIZATION_TRANSACTION_ID_REQUIRED("PAYMENT_0022", "Authorization transaction id required"),
    /** Authorized at required */
    AUTHORIZED_AT_REQUIRED("PAYMENT_0023", "Authorized at required"),
    /** Unsupported outbox event type */
    UNSUPPORTED_OUTBOX_EVENT_TYPE("PAYMENT_0024", "Unsupported outbox event type"),
    /** Payment must be capturing to recover */
    PAYMENT_MUST_BE_CAPTURING_TO_RECOVER("PAYMENT_0025", "Payment must be capturing to recover"),

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
    PaymentErrorCode(String code, String defaultMessage) {
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
