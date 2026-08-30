package io.asteria.payment.infrastructure.channel;

import io.asteria.payment.application.port.channel.*;
import io.asteria.payment.domain.enums.PaymentMethod;
import org.springframework.stereotype.Component;

/**
 * 银行卡支付渠道
 */
@Component
public class CardPaymentChannel implements PaymentChannel {

    /**
     * 判断是否支持指定支付方式
     */
    @Override
    public boolean supports(PaymentMethod paymentMethod) {
        return paymentMethod == PaymentMethod.CARD;
    }

    /**
     * 发起支付授权
     */
    @Override
    public AuthorizationResult authorize(AuthorizationRequest request) {
        return AuthorizationResult.success("auth_" + request.paymentId().value());
    }

    /**
     * 发起支付捕获
     */
    @Override
    public CaptureResult capture(CaptureRequest request) {
        return CaptureResult.success("capture_" + request.paymentId().value());
    }
}