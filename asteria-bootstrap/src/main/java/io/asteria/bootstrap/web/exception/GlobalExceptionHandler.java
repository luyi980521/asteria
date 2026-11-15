package io.asteria.bootstrap.web.exception;

import io.asteria.balance.domain.exception.BalanceDomainException;
import io.asteria.common.domain.exception.CommonDomainException;
import io.asteria.common.domain.exception.BusinessException;
import io.asteria.common.application.exception.RemoteServiceException;
import io.asteria.currency.domain.exception.CurrencyDomainException;
import io.asteria.ledger.domain.exception.LedgerDomainException;
import io.asteria.payment.domain.exception.PaymentDomainException;
import io.asteria.settlement.domain.exception.SettlementDomainException;
import io.asteria.web.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps domain failures to the application's public HTTP response contract.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 保留余额模块异常的响应与日志契约。
     *
     * @param exception 余额业务异常
     * @return 原有错误响应
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(BalanceDomainException.class)
    public ApiResponse<Void> handleBalanceDomainException(BalanceDomainException exception) {
        log.warn("Balance domain exception, code: {}, message: {}",
                exception.errorCode().code(), exception.getMessage(), exception);
        return ApiResponse.failure(exception.errorCode().code(), exception.getMessage());
    }

    /**
     * Preserves downstream error details using the common HTTP envelope.
     */
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    @ExceptionHandler(RemoteServiceException.class)
    public ApiResponse<Void> handleRemoteServiceException(RemoteServiceException exception) {
        log.warn("Remote service exception, code: {}, message: {}",
                exception.code(), exception.getMessage(), exception);
        return ApiResponse.failure(exception.code(), exception.getMessage());
    }

    /**
     * Handles shared value object validation failures.
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(CommonDomainException.class)
    public ApiResponse<Void> handleCommonDomainException(CommonDomainException exception) {
        log.warn("Common domain exception, code: {}, message: {}",
                exception.errorCode().code(), exception.getMessage(), exception);
        return ApiResponse.failure(exception.errorCode().code(), exception.getMessage());
    }

    /**
     * Handles currency master data failures.
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(CurrencyDomainException.class)
    public ApiResponse<Void> handleCurrencyDomainException(CurrencyDomainException exception) {
        log.warn("Currency domain exception, code: {}, message: {}",
                exception.errorCode().code(), exception.getMessage(), exception);
        return ApiResponse.failure(exception.errorCode().code(), exception.getMessage());
    }

    /**
     * Handles ledger failures, including duplicate accounts.
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(LedgerDomainException.class)
    public ApiResponse<Void> handleLedgerDomainException(LedgerDomainException exception) {
        log.warn("Ledger domain exception, code: {}, message: {}",
                exception.errorCode().code(), exception.getMessage(), exception);
        return ApiResponse.failure(exception.errorCode().code(), exception.getMessage());
    }

    /**
     * Handles payment failures.
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(PaymentDomainException.class)
    public ApiResponse<Void> handlePaymentDomainException(PaymentDomainException exception) {
        log.warn("Payment domain exception, code: {}, message: {}",
                exception.errorCode().code(), exception.getMessage(), exception);
        return ApiResponse.failure(exception.errorCode().code(), exception.getMessage());
    }

    /**
     * Handles settlement failures using the module's existing error accessor.
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(SettlementDomainException.class)
    public ApiResponse<Void> handleSettlementDomainException(SettlementDomainException exception) {
        log.warn("Settlement domain exception, code: {}, message: {}",
                exception.getErrorCode().getCode(), exception.getMessage(), exception);
        return ApiResponse.failure(exception.getErrorCode().getCode(), exception.getMessage());
    }

    /**
     * 识别公共业务异常，保留原先未配置模块处理器时的 HTTP 500 与 ERROR 日志行为。
     * 具体模块处理器由 Spring 按异常类型距离优先匹配。
     *
     * @param exception 尚无模块专用处理器的业务异常
     * @return 原有系统错误响应，不暴露内部诊断信息
     */
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(BusinessException.class)
    public ApiResponse<Void> handleBusinessException(BusinessException exception) {
        return handleException(exception);
    }

    /**
     * Logs unexpected failures while keeping internal details out of the response.
     */
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleException(Exception exception) {
        log.error("Unexpected exception", exception);
        return ApiResponse.failure("SYSTEM_ERROR", "Internal server error");
    }
}
