package io.asteria.payment.application.port.channel;

/**
 * 支付捕获结果
 */
public record CaptureResult(
        boolean success,
        String channelTransactionId,
        String failureCode,
        String failureMessage
) {

    /** No definitive channel outcome; retain the reservation and local in-flight state. */
    public static CaptureResult unknown() {
        return new CaptureResult(false, null, null, null);
    }

    public boolean isUnknown() {
        return !success && (failureCode == null || failureCode.isBlank()
                || "UNKNOWN".equalsIgnoreCase(failureCode) || "TIMEOUT".equalsIgnoreCase(failureCode));
    }

    /**
     * 创建捕获成功结果
     */
    public static CaptureResult success(String channelTransactionId) {
        return new CaptureResult(true, channelTransactionId, null, null);
    }

    /**
     * 创建捕获失败结果
     */
    public static CaptureResult failure(String failureCode, String failureMessage) {
        return new CaptureResult(false, null, failureCode, failureMessage);
    }
}
