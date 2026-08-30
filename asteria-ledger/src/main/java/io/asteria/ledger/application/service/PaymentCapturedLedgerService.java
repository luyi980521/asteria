package io.asteria.ledger.application.service;

import io.asteria.ledger.application.message.PaymentCapturedMessage;

/**
 * 支付捕获事件账本功能接口定义
 * */
public interface PaymentCapturedLedgerService {

    /**
     * 处理支付捕获事件
     * */
    void handle(PaymentCapturedMessage message);
}
