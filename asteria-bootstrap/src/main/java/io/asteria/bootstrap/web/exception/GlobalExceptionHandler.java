package io.asteria.bootstrap.web.exception;

import io.asteria.common.domain.exception.CommonDomainException;
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
     * Logs unexpected failures while keeping internal details out of the response.
     */
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleException(Exception exception) {
        log.error("Unexpected exception", exception);
        return ApiResponse.failure("SYSTEM_ERROR", "Internal server error");
    }
}
