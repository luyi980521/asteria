package io.asteria.bootstrap.web;

import io.asteria.balance.domain.error.BalanceErrorCode;
import io.asteria.balance.domain.exception.BalanceDomainException;
import io.asteria.bootstrap.web.exception.GlobalExceptionHandler;
import io.asteria.common.application.exception.RemoteServiceException;
import io.asteria.common.domain.error.CommonErrorCode;
import io.asteria.common.domain.error.ErrorCode;
import io.asteria.common.domain.exception.BusinessException;
import io.asteria.common.domain.exception.CommonDomainException;
import io.asteria.common.trace.TraceConstants;
import io.asteria.currency.domain.error.CurrencyErrorCode;
import io.asteria.currency.domain.exception.CurrencyDomainException;
import io.asteria.ledger.domain.error.LedgerErrorCode;
import io.asteria.ledger.domain.exception.LedgerDomainException;
import io.asteria.ledger.infrastructure.messaging.consumer.PaymentCapturedConsumer;
import io.asteria.ledger.infrastructure.messaging.consumer.SettlementCompletedConsumer;
import io.asteria.payment.domain.error.PaymentErrorCode;
import io.asteria.payment.domain.exception.PaymentDomainException;
import io.asteria.reconciliation.domain.error.ReconciliationErrorCode;
import io.asteria.reconciliation.domain.exception.ReconciliationDomainException;
import io.asteria.settlement.domain.error.SettlementErrorCode;
import io.asteria.settlement.domain.exception.SettlementChannelUnknownException;
import io.asteria.settlement.domain.exception.SettlementDomainException;
import io.asteria.web.response.ApiResponse;
import org.junit.jupiter.api.Test;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.interceptor.DefaultTransactionAttribute;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.ExceptionHandlerMethodResolver;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证模块异常继承兼容、构造契约及 Spring 全局处理器优先级。
 */
class BusinessExceptionCompatibilityTest {
    /**
     * 提供全部模块异常的原有单参数构造样本。
     *
     * @return 各模块异常样本
     */
    private List<BusinessException> examples() {
        return List.of(
                new CommonDomainException(CommonErrorCode.NULL_ARGUMENT),
                new BalanceDomainException(BalanceErrorCode.INVALID_AMOUNT),
                new CurrencyDomainException(CurrencyErrorCode.CURRENCY_NOT_FOUND),
                new LedgerDomainException(LedgerErrorCode.INVALID_MONEY_AMOUNT),
                new PaymentDomainException(PaymentErrorCode.PAYMENT_NOT_FOUND),
                new ReconciliationDomainException(ReconciliationErrorCode.INVALID_PARAMS),
                new SettlementDomainException(SettlementErrorCode.INVALID_SETTLEMENT_AMOUNT),
                new SettlementChannelUnknownException(SettlementErrorCode.SETTLEMENT_CHANNEL_UNKNOW));
    }

    /**
     * 检查全部已有公开构造器，保留详细消息、cause 和单参数构造的 initCause 行为。
     *
     * @throws Exception 反射构造失败
     */
    @Test
    void originalConstructorsAndRuntimeRollbackRemainCompatible() throws Exception {
        for (BusinessException example : examples()) {
            ErrorCode code = example.errorCode();
            String message = code instanceof SettlementErrorCode value ? value.name() : code.defaultMessage();
            assertEquals(message, example.getMessage());
            assertTrue(new DefaultTransactionAttribute().rollbackOn(example));
            assertSame(code, example.getClass().getMethod(
                    code instanceof SettlementErrorCode ? "getErrorCode" : "errorCode").invoke(example));
            for (var constructor : example.getClass().getConstructors()) {
                var parameters = constructor.getParameterTypes();
                Throwable cause = new IllegalStateException("original cause");
                BusinessException constructed;
                if (parameters.length == 1) {
                    constructed = (BusinessException) constructor.newInstance(code);
                    if (example instanceof BalanceDomainException || code instanceof SettlementErrorCode) {
                        constructed.initCause(cause);
                        assertSame(cause, constructed.getCause());
                    } else {
                        assertThrows(IllegalStateException.class, () -> constructed.initCause(cause));
                    }
                    assertEquals(message, constructed.getMessage());
                } else if (parameters[1] == String.class) {
                    constructed = (BusinessException) constructor.newInstance(code, "detail");
                    assertEquals("detail", constructed.getMessage());
                    assertNull(constructed.getCause());
                } else {
                    constructed = (BusinessException) constructor.newInstance(code, cause);
                    assertEquals(message, constructed.getMessage());
                    assertSame(cause, constructed.getCause());
                }
                assertSame(code, constructed.errorCode());
                assertTrue(example.getClass().isInstance(constructed));
            }
        }
    }

    /**
     * 保留各模块不同的空错误码处理规则。
     */
    @Test
    void nullErrorCodePoliciesRemainUnchanged() {
        assertEquals(CommonErrorCode.NULL_ARGUMENT,
                assertThrows(CommonDomainException.class, () -> new CommonDomainException(null)).errorCode());
        assertEquals(BalanceErrorCode.BALANCE_ERROR_CODE_REQUIRED,
                assertThrows(BalanceDomainException.class, () -> new BalanceDomainException(null)).errorCode());
        assertEquals(CurrencyErrorCode.NULL_ARGUMENT,
                assertThrows(CurrencyDomainException.class, () -> new CurrencyDomainException(null)).errorCode());
        assertEquals(LedgerErrorCode.NULL_ARGUMENT,
                assertThrows(LedgerDomainException.class, () -> new LedgerDomainException(null)).errorCode());
        assertEquals(PaymentErrorCode.PAYMENT_ERROR_CODE_REQUIRED,
                assertThrows(PaymentDomainException.class, () -> new PaymentDomainException(null)).errorCode());
        assertEquals(ReconciliationErrorCode.INVALID_PARAMS, new ReconciliationDomainException(null).errorCode());
        assertThrows(NullPointerException.class, () -> new SettlementDomainException(null));
        assertThrows(NullPointerException.class, () -> new SettlementChannelUnknownException(null));
    }

    /**
     * 使用 Spring 实际解析器验证专用处理器优先，并核对响应与 HTTP 状态契约。
     *
     * @throws Exception 处理器反射调用失败
     */
    @Test
    void specificHandlersRetainPriorityAndExistingResponses() throws Exception {
        var resolver = new ExceptionHandlerMethodResolver(GlobalExceptionHandler.class);
        var handler = new GlobalExceptionHandler();
        for (BusinessException exception : examples()) {
            var method = resolver.resolveMethod(exception);
            assertNotNull(method);
            boolean fallback = exception instanceof ReconciliationDomainException
                    || exception instanceof SettlementChannelUnknownException;
            assertEquals(fallback ? BusinessException.class : exception.getClass(), method.getParameterTypes()[0]);
            assertEquals(fallback ? HttpStatus.INTERNAL_SERVER_ERROR : HttpStatus.BAD_REQUEST,
                    method.getAnnotation(ResponseStatus.class).value());
            var response = (ApiResponse<?>) method.invoke(handler, exception);
            assertFalse(response.success());
            assertNull(response.data());
            assertEquals(fallback ? "SYSTEM_ERROR" : exception.errorCode().code(), response.code());
            assertEquals(fallback ? "Internal server error" : exception.getMessage(), response.message());
        }
        var remote = resolver.resolveMethod(new RemoteServiceException("REMOTE", "remote failure"));
        assertNotNull(remote);
        assertEquals(HttpStatus.BAD_GATEWAY, remote.getAnnotation(ResponseStatus.class).value());
        var generic = resolver.resolveMethod(new BusinessException(CommonErrorCode.NULL_ARGUMENT, "internal"));
        assertNotNull(generic);
        assertEquals("handleBusinessException", generic.getName());
    }

    /**
     * 验证原有模块 catch 与 instanceof 判断仍然识别相同异常实例。
     */
    @Test
    void moduleCatchAndInstanceofRemainEffective() {
        var failure = new PaymentDomainException(PaymentErrorCode.PAYMENT_NOT_FOUND);
        try {
            throw failure;
        } catch (PaymentDomainException caught) {
            assertSame(failure, caught);
            assertInstanceOf(BusinessException.class, caught);
            assertTrue(((RuntimeException) caught) instanceof PaymentDomainException);
        }
        var unknown = new SettlementChannelUnknownException(SettlementErrorCode.SETTLEMENT_CHANNEL_UNKNOW);
        try {
            throw unknown;
        } catch (SettlementChannelUnknownException caught) {
            assertSame(unknown, caught);
            assertSame(SettlementErrorCode.SETTLEMENT_CHANNEL_UNKNOW, caught.getErrorCode());
        }
    }

    /**
     * 验证业务异常仍以原实例传播至 Kafka 容器，且消费失败时清理链路上下文。
     */
    @Test
    void kafkaConsumersStillPropagateModuleFailures() {
        var failure = new LedgerDomainException(LedgerErrorCode.INVALID_MONEY_AMOUNT);
        var record = new ConsumerRecord<String, String>("topic", 0, 0, "key", "{}");
        try {
            assertSame(failure, assertThrows(LedgerDomainException.class,
                    () -> new PaymentCapturedConsumer(message -> { throw failure; }).consume(record)));
            assertNull(MDC.get(TraceConstants.TRACE_ID));
            assertSame(failure, assertThrows(LedgerDomainException.class,
                    () -> new SettlementCompletedConsumer(message -> { throw failure; }).consume(record)));
            assertNull(MDC.get(TraceConstants.TRACE_ID));
            assertFalse(BusinessException.class.isAssignableFrom(RemoteServiceException.class));
        } finally {
            MDC.remove(TraceConstants.TRACE_ID);
        }
    }
}
