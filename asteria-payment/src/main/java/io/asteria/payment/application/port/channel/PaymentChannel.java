package io.asteria.payment.application.port.channel;

import io.asteria.payment.domain.entity.Payment;
import io.asteria.payment.domain.enums.PaymentMethod;

/**
 * 支付渠道
 * */
public interface PaymentChannel {

    /**
     * 判断是否支持指定支付方式
     */
    boolean supports(PaymentMethod paymentMethod);

    /**
     * 发起支付授权
     * */
    AuthorizationResult authorize(AuthorizationRequest request);

    /**
     * 发起支付捕获
     */
    CaptureResult capture(CaptureRequest request);

    /**
     * 查询外部渠道捕获状态
     * */
    CaptureQueryResult queryCapture(Payment payment);
}