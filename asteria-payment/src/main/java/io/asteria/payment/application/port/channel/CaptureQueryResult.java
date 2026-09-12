package io.asteria.payment.application.port.channel;

import io.asteria.payment.domain.enums.CaptureQueryStatus;

/**
 * 捕获查询结果
 *
 * @param status               查询状态
 * @param channelTransactionId 渠道交易ID
 * @param failureCode          失败码
 * @param failureMessage       失败信息
 */
public record CaptureQueryResult(
        CaptureQueryStatus status,
        String channelTransactionId,
        String failureCode,
        String failureMessage
) {

    /**
     * 捕获成功
     */
    public static CaptureQueryResult success(String channelTransactionId) {
        return new CaptureQueryResult(
                CaptureQueryStatus.SUCCESS,
                channelTransactionId,
                null,
                null
        );
    }

    /**
     * 捕获失败
     */
    public static CaptureQueryResult failed(String failureCode, String failureMessage) {
        return new CaptureQueryResult(
                CaptureQueryStatus.FAILED,
                null,
                failureCode,
                failureMessage
        );
    }

    /**
     * 捕获结果未知
     */
    public static CaptureQueryResult unknown() {
        return new CaptureQueryResult(
                CaptureQueryStatus.UNKNOWN,
                null,
                null,
                null
        );
    }

    /**
     * 是否捕获成功
     */
    public boolean isSuccess() {
        return status == CaptureQueryStatus.SUCCESS;
    }

    /**
     * 是否捕获失败
     */
    public boolean isFailed() {
        return status == CaptureQueryStatus.FAILED;
    }

    /**
     * 是否未知
     */
    public boolean isUnknown() {
        return status == CaptureQueryStatus.UNKNOWN;
    }
}