package io.asteria.payment.application.service.impl;

import io.asteria.common.application.port.DistributedIdGenerator;
import io.asteria.common.util.JsonUtils;
import io.asteria.payment.application.command.CreatePaymentCommand;
import io.asteria.payment.application.port.channel.*;
import io.asteria.payment.application.port.router.PaymentChannelRouter;
import io.asteria.payment.application.service.PaymentApplicationService;
import io.asteria.payment.domain.entity.Payment;
import io.asteria.payment.domain.error.PaymentErrorCode;
import io.asteria.payment.domain.exception.PaymentDomainException;
import io.asteria.payment.domain.repository.PaymentRepository;
import io.asteria.payment.domain.valueobject.PaymentId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * 支付功能接口定义实现类
 * */
@Slf4j
@Service
public class PaymentApplicationServiceImpl implements PaymentApplicationService {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private DistributedIdGenerator distributedIdGenerator;

    @Autowired
    private PaymentChannelRouter paymentChannelRouter;

    /**
     * 创建支付
     *
     * @param command {@link CreatePaymentCommand}
     * @return {@link PaymentId}
     */
    @Override
    public PaymentId create(CreatePaymentCommand command) {

        boolean isExists = paymentRepository.existsByReference(command.reference());
        if (isExists) {
            log.warn("The payment reference already exists: {}", JsonUtils.toJson(command.reference()));
            throw new PaymentDomainException(PaymentErrorCode.PAYMENT_REFERENCE_ALREADY_EXISTS);
        }

        PaymentId paymentId = PaymentId.of(distributedIdGenerator.nextId());
        Payment payment = Payment.create(
                paymentId,
                command.merchantId(),
                command.amount(),
                command.paymentMethod(),
                command.reference(),
                Instant.now()
        );
        paymentRepository.insert(payment);
        log.info("Payment created successfully, paymentId: {}, merchantId: {}, referenceType: {}, referenceId: {}",
                paymentId.value(), command.merchantId(),
                command.reference().referenceType(), command.reference().referenceId());
        return paymentId;
    }

    /**
     * 授权支付
     *
     * @param paymentId {@link PaymentId}
     */
    @Override
    public void authorize(PaymentId paymentId) {

        // 将支付流程推进为授权中并更新状态
        Payment payment = paymentRepository.findById(paymentId);
        payment.startAuthorization();
        paymentRepository.update(payment);

        AuthorizationRequest authorizationRequest = AuthorizationRequest.builder()
                .paymentId(paymentId)
                .amount(payment.getAmount())
                .paymentMethod(payment.getPaymentMethod())
                .build();
        PaymentChannel paymentChannel = paymentChannelRouter.route(payment);
        AuthorizationResult authorizationResult = paymentChannel.authorize(authorizationRequest);
        if (authorizationResult.success()) {
            payment.authorize(authorizationResult.channelTransactionId(), Instant.now());
        } else {
            payment.fail();
        }
        paymentRepository.update(payment);
        log.info("Payment authorized successfully: {}", paymentId);
    }

    /**
     * 捕获支付
     *
     * @param paymentId {@link PaymentId}
     */
    @Override
    public void capture(PaymentId paymentId) {

        // 将支付流程推进为捕获中并更新状态
        Payment payment = paymentRepository.findById(paymentId);
        payment.startCapture();
        paymentRepository.update(payment);

        CaptureRequest captureRequest = CaptureRequest.builder()
                .paymentId(paymentId)
                .amount(payment.getAmount())
                .authorizationTransactionId(payment.getAuthorizationTransactionId())
                .build();
        PaymentChannel paymentChannel = paymentChannelRouter.route(payment);
        CaptureResult captureResult = paymentChannel.capture(captureRequest);
        if (captureResult.success()) {
            payment.capture(Instant.now());
        } else {
            // 暂时先将捕获失败的支付单状态更改回 authorized，未来增加捕获失败的处理
            payment.captureFailed();
        }
        paymentRepository.update(payment);
        log.info("Payment capture successfully: {}", paymentId);
    }
}
