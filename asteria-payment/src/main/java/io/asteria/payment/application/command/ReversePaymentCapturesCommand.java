package io.asteria.payment.application.command;

import java.util.List;

/**
 * 捕获账务冲正命令
 * */
public record ReversePaymentCapturesCommand(
        List<Long> paymentIds,
        String reason
) {
}