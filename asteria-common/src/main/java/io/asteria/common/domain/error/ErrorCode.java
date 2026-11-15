package io.asteria.common.domain.error;

/**
 * 业务异常共用的稳定错误码与默认消息契约。
 */
public interface ErrorCode {
    /**
     * 返回稳定的业务错误码。
     *
     * @return 业务错误码
     */
    String code();

    /**
     * 返回默认业务错误消息。
     *
     * @return 默认错误消息
     */
    String defaultMessage();
}
