package io.asteria.payment.application.service.impl;

import io.asteria.common.application.port.DistributedIdGenerator;
import io.asteria.common.util.JsonUtils;
import io.asteria.payment.application.command.CreatePaymentCommand;
import io.asteria.payment.application.port.channel.*;
import io.asteria.payment.application.port.router.PaymentChannelRouter;
import io.asteria.payment.application.service.PaymentApplicationService;
import io.asteria.payment.application.service.PaymentTransactionService;
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

    @Autowired
    private PaymentTransactionService paymentTransactionService;

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
        Payment payment = paymentTransactionService.startAuthorization(paymentId);

        AuthorizationRequest authorizationRequest = AuthorizationRequest.builder()
                .paymentId(paymentId)
                .amount(payment.getAmount())
                .paymentMethod(payment.getPaymentMethod())
                .build();
        PaymentChannel paymentChannel = paymentChannelRouter.route(payment);
        AuthorizationResult authorizationResult = paymentChannel.authorize(authorizationRequest);
        paymentTransactionService.completeAuthorization(paymentId, authorizationResult);
        if (authorizationResult.success()) {
            log.info("Authorization succeeded, paymentId: {}", paymentId.value());
        } else {
            log.error("Authorization failed, paymentId: {}, failureCode: {}",
                    paymentId.value(), authorizationResult.failureCode());
        }
    }

    /**
     * 捕获支付
     *
     * @param paymentId {@link PaymentId}
     */
    @Override
    public void capture(PaymentId paymentId) {

        // 将支付流程推进为捕获中并更新状态
        Payment payment = paymentTransactionService.startCapture(paymentId);

        CaptureRequest captureRequest = CaptureRequest.builder()
                .paymentId(paymentId)
                .amount(payment.getAmount())
                .authorizationTransactionId(payment.getAuthorizationTransactionId())
                .build();
        PaymentChannel paymentChannel = paymentChannelRouter.route(payment);
        CaptureResult captureResult = paymentChannel.capture(captureRequest);
        paymentTransactionService.completeCapture(paymentId, captureResult);
        if (captureResult.success()) {
            log.info("Capture succeeded, paymentId: {}", paymentId.value());
        } else {
            log.error("Capture failed, paymentId: {}, failureCode: {}",
                    paymentId.value(), captureResult.failureCode());
        }
    }
}
