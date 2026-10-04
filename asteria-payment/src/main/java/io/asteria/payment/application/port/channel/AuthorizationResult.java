package io.asteria.payment.application.port.channel;

/**
 * 支付授权结果
 */
public record AuthorizationResult(
        boolean success,
        String channelTransactionId,
        String failureCode,
        String failureMessage
) {

    /** No definitive channel outcome; retain the reservation and local in-flight state. */
    public static AuthorizationResult unknown() {
        return new AuthorizationResult(false, null, null, null);
    }

    /** Unknown/timeout codes and missing rejection evidence must not trigger release. */
    public boolean isUnknown() {
        return !success && (failureCode == null || failureCode.isBlank()
                || "UNKNOWN".equalsIgnoreCase(failureCode) || "TIMEOUT".equalsIgnoreCase(failureCode));
    }

    /**
     * 创建授权成功结果
     * */
    public static AuthorizationResult success(String channelTransactionId) {
        return new AuthorizationResult(true, channelTransactionId, null, null);
    }

    /**
     * 创建授权失败结果
     * */
    public static AuthorizationResult failure(String failureCode, String failureMessage) {
        return new AuthorizationResult(false, null, failureCode, failureMessage);
    }
}
