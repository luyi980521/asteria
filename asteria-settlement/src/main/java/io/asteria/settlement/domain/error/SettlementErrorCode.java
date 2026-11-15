package io.asteria.settlement.domain.error;

import io.asteria.common.domain.error.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Settlement 错误码
 */
@Getter
@RequiredArgsConstructor
public enum SettlementErrorCode implements ErrorCode {

    /** Invalid settlement amount */
    INVALID_SETTLEMENT_AMOUNT("SETTLEMENT_0001", "Invalid settlement amount"),
    /** Invalid settlement status */
    INVALID_SETTLEMENT_STATUS("SETTLEMENT_0002", "Invalid settlement status"),
    /** Settlement already exists */
    SETTLEMENT_ALREADY_EXISTS("SETTLEMENT_0003", "Settlement already exists"),
    /** Invalid settlement batch status */
    INVALID_SETTLEMENT_BATCH_STATUS("SETTLEMENT_0004", "Invalid settlement batch status"),
    /** Settlement items required */
    SETTLEMENT_ITEMS_REQUIRED("SETTLEMENT_0005", "Settlement items required"),
    /** Settlement batch already exists */
    SETTLEMENT_BATCH_ALREADY_EXISTS("SETTLEMENT_0006", "Settlement batch already exists"),
    /** Settlement channel not found */
    SETTLEMENT_CHANNEL_NOT_FOUND("SETTLEMENT_0007", "Settlement channel not found"),
    /** Settlement batch not found */
    SETTLEMENT_BATCH_NOT_FOUND("SETTLEMENT_0008", "Settlement batch not found"),
    /** Settlement channel unknow */
    SETTLEMENT_CHANNEL_UNKNOW("SETTLEMENT_0009", "Settlement channel unknow"),
    /** Unsupported outbox event type */
    UNSUPPORTED_OUTBOX_EVENT_TYPE("SETTLEMENT_0010", "Unsupported outbox event type"),

    ;

    /** 业务错误码 */
    private final String code;
    /** 默认错误消息 */
    private final String defaultMessage;

    /**
     * 返回稳定的结算业务错误码，保留 Lombok 生成的 getCode 访问器。
     *
     * @return 结算业务错误码
     */
    @Override
    public String code() {
        return code;
    }

    /**
     * 返回默认结算错误消息，保留 Lombok 生成的 getDefaultMessage 访问器。
     *
     * @return 默认错误消息
     */
    @Override
    public String defaultMessage() {
        return defaultMessage;
    }
}