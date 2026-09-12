package io.asteria.payment.application.service.impl;

import io.asteria.common.application.port.DistributedIdGenerator;
import io.asteria.common.util.JsonUtils;
import io.asteria.common.util.ServiceResponseUtils;
import io.asteria.payment.application.command.CreatePaymentCommand;
import io.asteria.payment.application.command.ReversePaymentCapturesCommand;
import io.asteria.payment.application.port.channel.*;
import io.asteria.payment.application.port.router.PaymentChannelRouter;
import io.asteria.payment.application.request.ReversePaymentCapturesRequest;
import io.asteria.payment.application.response.ReversePaymentCapturesResponse;
import io.asteria.payment.application.service.PaymentApplicationService;
import io.asteria.payment.application.service.PaymentTransactionService;
import io.asteria.payment.domain.entity.Payment;
import io.asteria.payment.domain.enums.PaymentOutboxEventType;
import io.asteria.payment.domain.enums.PaymentStatus;
import io.asteria.payment.domain.error.PaymentErrorCode;
import io.asteria.payment.domain.exception.PaymentDomainException;
import io.asteria.payment.domain.repository.PaymentOutboxEventRepository;
import io.asteria.payment.domain.repository.PaymentRepository;
import io.asteria.payment.domain.valueobject.PaymentId;
import io.asteria.payment.domain.valueobject.PaymentOutboxEvent;
import io.asteria.payment.infrastructure.client.LedgerClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * 支付功能接口定义实现类
 * */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentApplicationServiceImpl implements PaymentApplicationService {

    private final PaymentRepository paymentRepository;
    private final DistributedIdGenerator distributedIdGenerator;
    private final PaymentChannelRouter paymentChannelRouter;
    private final PaymentTransactionService paymentTransactionService;
    private final PaymentChannel paymentChannel;
    private final LedgerClient ledgerClient;
    private final PaymentOutboxEventRepository paymentOutboxEventRepository;

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

    /**
     * 回查渠道捕获结果并恢复本地支付状态
     */
    @Override
    public void recoverCapture(PaymentId paymentId) {

        Payment payment = paymentRepository.findById(paymentId);
        if (payment.getStatus() != PaymentStatus.CAPTURING) {
            log.warn("The payment must be capturing to recover: {}, {}", payment, payment.getStatus());
            throw new PaymentDomainException(PaymentErrorCode.PAYMENT_MUST_BE_CAPTURING_TO_RECOVER);
        }

        // 回查捕获状态并将内部状态更新为匹配的状态
        CaptureQueryResult captureQueryResult = paymentChannel.queryCapture(payment);
        if (captureQueryResult.isSuccess()) {
            paymentTransactionService.completeCapture(
                    paymentId, CaptureResult.success(captureQueryResult.channelTransactionId())
            );
            return;
        }

        if (captureQueryResult.isFailed()) {
            paymentTransactionService.completeCapture(
                    paymentId, CaptureResult.failure(
                            captureQueryResult.failureCode(), captureQueryResult.failureMessage()
                    )
            );
            return;
        }

        log.warn("Capture recovery remains unknown, paymentId: {}", paymentId.value());
    }

    /**
     * 捕获账务冲正
     */
    @Override
    public ReversePaymentCapturesResponse reverseCaptures(ReversePaymentCapturesCommand command) {

        List<String> aggregateIds = command.paymentIds().stream()
                .map(paymentId -> PaymentId.of(paymentId).value().toString())
                .distinct()
                .toList();
        List<PaymentOutboxEvent> events = paymentOutboxEventRepository.findByAggregateIds(aggregateIds);
        List<String> eventIds = events.stream()
                .filter(event -> event.getEventType() == PaymentOutboxEventType.PAYMENT_CAPTURED)
                .map(PaymentOutboxEvent::getEventId)
                .distinct()
                .toList();
        if (CollectionUtils.isEmpty(eventIds)) {
            log.warn("No capture events found for reversal, paymentIds: {}", command.paymentIds());
            return new ReversePaymentCapturesResponse(List.of());
        }
        ReversePaymentCapturesRequest request = ReversePaymentCapturesRequest.builder()
                .eventIds(eventIds)
                .reason(command.reason())
                .build();
        return ServiceResponseUtils.getData(ledgerClient.reverseJournalEntries(request));
    }
}
