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