package io.asteria.payment.application.controller;

import io.asteria.common.util.JsonUtils;
import io.asteria.payment.application.command.CreatePaymentCommand;
import io.asteria.payment.application.service.PaymentApplicationService;
import io.asteria.payment.domain.valueobject.PaymentId;
import io.asteria.web.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 支付功能接口定义
 * */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payment")
public class PaymentController {

    private final PaymentApplicationService paymentApplicationService;

    @PostMapping
    public ApiResponse<PaymentId> create(@RequestBody CreatePaymentCommand command) {
        log.info("Received create payment request: {}", JsonUtils.toJson(command));
        return ApiResponse.success(paymentApplicationService.create(command));
    }

    @PostMapping("/{id}/authorize")
    public ApiResponse<Void> authorize(@PathVariable("id") Long id) {
        log.info("Received authorize payment request: {}", id);
        paymentApplicationService.authorize(PaymentId.of(id));
        return ApiResponse.successWithoutData();
    }

    @PostMapping("/{id}/capture")
    public ApiResponse<Void> capture(@PathVariable("id") Long id) {
        log.info("Received capture payment request: {}", id);
        paymentApplicationService.capture(PaymentId.of(id));
        return ApiResponse.successWithoutData();
    }

    @PostMapping("/{id}/capture/recover")
    public ApiResponse<Void> recoverCapture(@PathVariable("id") Long id) {
        log.info("Received recover capture request: {}", id);
        PaymentId paymentId = PaymentId.of(id);
        paymentApplicationService.recoverCapture(paymentId);
        return ApiResponse.successWithoutData();
    }
}
