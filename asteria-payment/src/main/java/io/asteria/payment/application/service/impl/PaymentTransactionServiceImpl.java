package io.asteria.payment.application.service.impl;

import io.asteria.payment.application.assembler.PaymentOutboxEventAssembler;
import io.asteria.payment.application.port.channel.AuthorizationResult;
import io.asteria.payment.application.port.channel.CaptureResult;
import io.asteria.payment.application.service.PaymentTransactionService;
import io.asteria.payment.domain.entity.Payment;
import io.asteria.payment.domain.repository.PaymentOutboxEventRepository;
import io.asteria.payment.domain.repository.PaymentRepository;
import io.asteria.payment.domain.valueobject.PaymentOutboxEvent;
import io.asteria.payment.domain.valueobject.PaymentId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Payment 本地状态事务服务接口定义实现类
 * */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentTransactionServiceImpl implements PaymentTransactionService {

    private final PaymentRepository paymentRepository;

    private final PaymentOutboxEventAssembler paymentOutboxEventAssembler;

    private final PaymentOutboxEventRepository paymentOutboxEventRepository;

    /**
     * 开始授权并提交本地状态
     * */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Payment startAuthorization(PaymentId paymentId) {
        Payment payment = paymentRepository.findById(paymentId);
        payment.startAuthorization();
        paymentRepository.update(payment);
        return payment;
    }

    /**
     * 根据渠道结果完成授权并提交本地状态
     * */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeAuthorization(PaymentId paymentId, AuthorizationResult result) {
        Payment payment = paymentRepository.findById(paymentId);
        if (result.success()) {
            payment.authorize(result.channelTransactionId(), Instant.now());
        } else {
            payment.fail();
        }
        paymentRepository.update(payment);
    }

    /**
     * 开始捕获并提交本地状态
     * */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Payment startCapture(PaymentId paymentId) {
        Payment payment = paymentRepository.findById(paymentId);
        payment.startCapture();
        paymentRepository.update(payment);
        return payment;
    }

    /**
     * 根据渠道结果完成捕获并提交本地状态
     * */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeCapture(PaymentId paymentId, CaptureResult result) {
        Payment payment = paymentRepository.findById(paymentId);
        if (result.success()) {
            payment.capture(Instant.now());
        } else {
            // 暂时先将捕获失败的支付单状态更改回 authorized，未来增加捕获失败的处理
            payment.captureFailed();
        }

        PaymentOutboxEvent paymentOutboxEvent = paymentOutboxEventAssembler.toPaymentCapturedOutboxEvent(payment);
        paymentOutboxEventRepository.insert(paymentOutboxEvent);
        paymentRepository.update(payment);
        log.info("Payment capture completed, paymentId: {}, outboxEventId: {}",
                paymentId.value(), paymentOutboxEvent.getEventId());
    }
}
