package io.asteria.bootstrap.web;

import io.asteria.bootstrap.web.exception.GlobalExceptionHandler;
import io.asteria.common.domain.error.CommonErrorCode;
import io.asteria.common.domain.exception.CommonDomainException;
import io.asteria.common.domain.valueobject.CurrencyCode;
import io.asteria.common.util.JsonUtils;
import io.asteria.currency.domain.error.CurrencyErrorCode;
import io.asteria.currency.domain.exception.CurrencyDomainException;
import io.asteria.ledger.application.command.CreateLedgerAccountCommand;
import io.asteria.ledger.application.controller.LedgerAccountController;
import io.asteria.ledger.application.service.LedgerAccountApplicationService;
import io.asteria.ledger.domain.error.LedgerErrorCode;
import io.asteria.ledger.domain.exception.LedgerDomainException;
import io.asteria.ledger.domain.valueobject.LedgerAccountId;
import io.asteria.payment.domain.error.PaymentErrorCode;
import io.asteria.payment.domain.exception.PaymentDomainException;
import io.asteria.settlement.domain.error.SettlementErrorCode;
import io.asteria.settlement.domain.exception.SettlementDomainException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies the public HTTP envelope without accessing PostgreSQL or Kafka.
 */
class LedgerAccountHttpResponseTest {
    private static final String REQUEST = """
            {"accountCode":"PAYMENT_RECEIVABLE_USD","ownerType":"SYSTEM","ownerId":1,
             "category":"ASSET","currency":"USD","allowNegativeBalance":false}
            """;
    private LedgerAccountApplicationService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(LedgerAccountApplicationService.class);
        mvc = MockMvcBuilders.standaloneSetup(new LedgerAccountController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void successfulCreationWrapsTheExistingIdResponse() throws Exception {
        when(service.createLedgerAccount(any())).thenReturn(LedgerAccountId.of(1001L));
        String body = mvc.perform(post("/api/ledger/accounts")
                        .contentType(MediaType.APPLICATION_JSON).content(REQUEST))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertEquals(JsonUtils.readTree("""
                {"success":true,"code":"SUCCESS","message":"Success","data":{"value":1001}}
                """), JsonUtils.readTree(body));
        var command = ArgumentCaptor.forClass(CreateLedgerAccountCommand.class);
        verify(service).createLedgerAccount(command.capture());
        assertEquals("PAYMENT_RECEIVABLE_USD", command.getValue().accountCode());
        assertEquals(CurrencyCode.of("USD"), command.getValue().currency());
    }

    @Test
    void duplicateAccountExceptionReturns400AndExactLedgerError() throws Exception {
        when(service.createLedgerAccount(any())).thenThrow(
                new LedgerDomainException(LedgerErrorCode.LEDGER_ACCOUNT_ALREADY_EXISTS));
        String body = mvc.perform(post("/api/ledger/accounts")
                        .contentType(MediaType.APPLICATION_JSON).content(REQUEST))
                .andExpect(status().isBadRequest()).andReturn().getResponse().getContentAsString();
        assertEquals(JsonUtils.readTree("""
                {"success":false,"code":"LEDGER_0030",
                 "message":"Ledger account is already exists","data":null}
                """), JsonUtils.readTree(body));
    }

    @ParameterizedTest
    @MethodSource("domainFailures")
    void allDomainExceptionsUse400AndPreserveCodeAndMessage(Failure failure) throws Exception {
        when(service.createLedgerAccount(any())).thenThrow(failure.exception());
        String body = mvc.perform(post("/api/ledger/accounts")
                        .contentType(MediaType.APPLICATION_JSON).content(REQUEST))
                .andExpect(status().isBadRequest()).andReturn().getResponse().getContentAsString();
        var json = JsonUtils.readTree(body);
        assertEquals(4, json.size());
        assertFalse(json.path("success").booleanValue());
        assertEquals(failure.code(), json.path("code").textValue());
        assertEquals(failure.exception().getMessage(), json.path("message").textValue());
        assertTrue(json.get("data").isNull());
    }

    @Test
    void unexpectedFailureReturns500WithoutInternalDetails() throws Exception {
        when(service.createLedgerAccount(any())).thenThrow(new IllegalStateException("private database details"));
        String body = mvc.perform(post("/api/ledger/accounts")
                        .contentType(MediaType.APPLICATION_JSON).content(REQUEST))
                .andExpect(status().isInternalServerError()).andReturn().getResponse().getContentAsString();
        assertEquals(JsonUtils.readTree("""
                {"success":false,"code":"SYSTEM_ERROR","message":"Internal server error","data":null}
                """), JsonUtils.readTree(body));
    }

    @Test
    void currencyValidationInControllerUsesCommonHandler() throws Exception {
        String body = mvc.perform(post("/api/ledger/accounts")
                        .contentType(MediaType.APPLICATION_JSON).content(REQUEST.replace("\"USD\"", "\"US\"")))
                .andExpect(status().isBadRequest()).andReturn().getResponse().getContentAsString();
        assertEquals("COMMON_0009", JsonUtils.readTree(body).path("code").textValue());
        verifyNoInteractions(service);
    }

    private static Stream<Failure> domainFailures() {
        return Stream.of(
                new Failure(new CommonDomainException(CommonErrorCode.NULL_ARGUMENT, "Missing input"), "COMMON_0001"),
                new Failure(new CurrencyDomainException(CurrencyErrorCode.CURRENCY_NOT_FOUND), "CURRENCY_0008"),
                new Failure(new LedgerDomainException(LedgerErrorCode.LEDGER_ACCOUNT_ALREADY_EXISTS), "LEDGER_0030"),
                new Failure(new PaymentDomainException(PaymentErrorCode.PAYMENT_NOT_FOUND), "PAYMENT_0017"),
                new Failure(new SettlementDomainException(SettlementErrorCode.SETTLEMENT_BATCH_NOT_FOUND), "SETTLEMENT_0008")
        );
    }

    private record Failure(RuntimeException exception, String code) {
    }
}
