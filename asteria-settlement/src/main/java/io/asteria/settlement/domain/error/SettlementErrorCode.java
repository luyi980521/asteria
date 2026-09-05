package io.asteria.settlement.domain.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Settlement 错误码
 */
@Getter
@RequiredArgsConstructor
public enum SettlementErrorCode {

    INVALID_SETTLEMENT_AMOUNT("SETTLEMENT_0001", "Invalid settlement amount"),
    INVALID_SETTLEMENT_STATUS("SETTLEMENT_0002", "Invalid settlement status"),
    SETTLEMENT_ALREADY_EXISTS("SETTLEMENT_0003", "Settlement already exists"),
    INVALID_SETTLEMENT_BATCH_STATUS("SETTLEMENT_0004", "Invalid settlement batch status"),
    SETTLEMENT_ITEMS_REQUIRED("SETTLEMENT_0005", "Settlement items required"),
    SETTLEMENT_BATCH_ALREADY_EXISTS("SETTLEMENT_0006", "Settlement batch already exists"),
    SETTLEMENT_CHANNEL_NOT_FOUND("SETTLEMENT_0007", "Settlement channel not found"),
    SETTLEMENT_BATCH_NOT_FOUND("SETTLEMENT_0008", "Settlement batch not found"),
    SETTLEMENT_CHANNEL_UNKNOW("SETTLEMENT_0009", "Settlement channel unknow"),

    ;

    private final String code;
    private final String defaultMessage;

}
