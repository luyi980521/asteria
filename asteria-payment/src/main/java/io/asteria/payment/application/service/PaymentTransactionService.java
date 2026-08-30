package io.asteria.payment.application.service;

import io.asteria.payment.application.port.channel.AuthorizationResult;
import io.asteria.payment.application.port.channel.CaptureResult;
import io.asteria.payment.domain.entity.Payment;
import io.asteria.payment.domain.valueobject.PaymentId;

/**
 * Payment 本地状态事务服务功能接口定义
 * */
public interface PaymentTransactionService {

    /**
     * 开始授权并提交本地状态
     * */
    Payment startAuthorization(PaymentId paymentId);

    /**
     * 根据渠道结果完成授权并提交本地状态
     * */
    void completeAuthorization(PaymentId paymentId, AuthorizationResult result);

    /**
     * 开始捕获并提交本地状态
     * */
    Payment startCapture(PaymentId paymentId);

    /**
     * 根据渠道结果完成捕获并提交本地状态
     * */
    void completeCapture(PaymentId paymentId, CaptureResult result);
}
