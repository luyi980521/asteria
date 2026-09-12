package io.asteria.payment.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 捕获账务冲正状态枚举定义
 * */
@Getter
@AllArgsConstructor
public enum ReversePaymentCaptureStatus {

    REVERSED,
    ALREADY_REVERSED,
    FAILED,

    ;
}
