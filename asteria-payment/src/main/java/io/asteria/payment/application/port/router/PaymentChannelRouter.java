package io.asteria.payment.application.port.router;

import io.asteria.payment.application.port.channel.PaymentChannel;
import io.asteria.payment.domain.entity.Payment;
import io.asteria.payment.domain.error.PaymentErrorCode;
import io.asteria.payment.domain.exception.PaymentDomainException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 支付渠道路由器
 */
@Component
@RequiredArgsConstructor
public class PaymentChannelRouter {

    /** 支付渠道集合 */
    private final List<PaymentChannel> paymentChannels;

    /**
     * 路由支付渠道
     */
    public PaymentChannel route(Payment payment) {
        return paymentChannels.stream()
                .filter(channel -> channel.supports(payment.getPaymentMethod()))
                .findFirst()
                .orElseThrow(() -> new PaymentDomainException(PaymentErrorCode.PAYMENT_CHANNEL_NOT_FOUND));
    }
}