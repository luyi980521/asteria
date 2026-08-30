package io.asteria.payment.application.service;

import io.asteria.payment.application.command.CreatePaymentCommand;
import io.asteria.payment.domain.valueobject.PaymentId;

/**
 * 支付功能接口定义
 * */
public interface PaymentApplicationService {

    /**
     * 创建支付
     *
     * @param command {@link CreatePaymentCommand}
     * @return {@link PaymentId}
     * */
    PaymentId create(CreatePaymentCommand command);

    /**
     * 授权支付
     *
     * @param paymentId {@link PaymentId}
     * */
    void authorize(PaymentId paymentId);

    /**
     * 捕获支付
     *
     * @param paymentId {@link PaymentId}
     * */
    void capture(PaymentId paymentId);
}
