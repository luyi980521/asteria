package io.asteria.payment.application.response;

import io.asteria.payment.domain.enums.ReversePaymentCaptureStatus;
import lombok.Builder;

import java.util.List;

@Builder
public record ReversePaymentCapturesResponse(List<Result> results) {

    @Builder
    public record Result(String eventId, ReversePaymentCaptureStatus status, String code, String message) {
    }
}
