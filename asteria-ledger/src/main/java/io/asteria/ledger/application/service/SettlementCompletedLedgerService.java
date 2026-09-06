package io.asteria.ledger.application.service;

import io.asteria.ledger.application.message.SettlementCompletedMessage;

/**
 * 结算完成账本功能接口定义
 * */
public interface SettlementCompletedLedgerService {

    /**
     * 处理支付捕获事件
     * */
    void handle(SettlementCompletedMessage message);
}
