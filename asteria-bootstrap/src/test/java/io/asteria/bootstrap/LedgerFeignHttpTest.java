package io.asteria.bootstrap;

import io.asteria.ledger.application.request.ReverseJournalEntriesRequest;
import io.asteria.ledger.application.response.ReverseJournalEntriesResponse;
import io.asteria.common.domain.valueobject.CurrencyCode;
import io.asteria.common.domain.valueobject.Money;
import io.asteria.ledger.domain.entity.JournalEntry;
import io.asteria.ledger.domain.entity.Posting;
import io.asteria.ledger.domain.enums.DebitCredit;
import io.asteria.ledger.domain.repository.JournalEntryRepository;
import io.asteria.ledger.domain.valueobject.*;
import io.asteria.payment.infrastructure.client.LedgerClient;
import io.asteria.web.response.ApiResponse;
import feign.FeignException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Exercises the registered Feign proxy, real HTTP controller and application service. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "asteria.service.ledger.url=http://localhost:${local.server.port}",
        "spring.cloud.openfeign.lazy-attributes-resolution=true",
        "asteria.payment.outbox.publish-task-enabled=false",
        "spring.kafka.listener.auto-startup=false"
})
class LedgerFeignHttpTest {
    @Autowired
    private ApplicationContext context;
    @MockitoBean
    private JournalEntryRepository repository;
    @MockitoBean
    private PlatformTransactionManager transactionManager;

    @Test
    void batchReturnsIndividualResultsAndRepeatedCallIsIdempotent() {
        when(transactionManager.getTransaction(any(TransactionDefinition.class)))
                .thenAnswer(invocation -> new SimpleTransactionStatus());
        CurrencyCode currency = CurrencyCode.of("USD");
        Money money = Money.of(new BigDecimal("100.00"), currency);
        JournalEntry entry = JournalEntry.create(JournalEntryId.of(1001L), List.of(
                Posting.create(PostingId.of(2001L), LedgerAccountId.of(3001L), money, DebitCredit.DEBIT),
                Posting.create(PostingId.of(2002L), LedgerAccountId.of(3002L), money, DebitCredit.CREDIT)),
                new JournalReference("PAYMENT", "payment-1", "PAYMENT_CAPTURED", "captured-1"));
        entry.post(Instant.now().minusSeconds(60));
        when(repository.findByEventIdForUpdate("captured-1")).thenReturn(Optional.of(entry));
        when(repository.findByEventIdForUpdate("missing")).thenReturn(Optional.empty());
        when(repository.findByEventIdForUpdate("broken")).thenThrow(new IllegalStateException("private details"));

        LedgerClient client = context.getBean(LedgerClient.class);
        ApiResponse<ReverseJournalEntriesResponse> response = client.reverseJournalEntries(new ReverseJournalEntriesRequest(
                List.of("broken", "captured-1", "missing", "captured-1"), "Manual recovery"));

        assertTrue(response.success());
        assertEquals(List.of("FAILED", "REVERSED", "FAILED", "ALREADY_REVERSED"),
                response.data().results().stream().map(result -> result.status()).toList());
        assertEquals("SYSTEM_ERROR", response.data().results().getFirst().code());
        assertFalse(response.data().results().getFirst().message().contains("private details"));
        assertEquals("JOURNAL_ENTRY_NOT_FOUND", response.data().results().get(2).code());
        assertEquals("ALREADY_REVERSED", client.reverseJournalEntries(
                new ReverseJournalEntriesRequest(List.of("captured-1"), "Retry"))
                .data().results().getFirst().status());
        verify(repository, times(1)).insert(any());
        verify(repository, times(1)).update(entry);
        verify(transactionManager, times(1)).rollback(any());
        verify(transactionManager, times(4)).commit(any());
        verify(transactionManager, times(5)).getTransaction(argThat(definition ->
                definition.getPropagationBehavior() == TransactionDefinition.PROPAGATION_REQUIRES_NEW));
    }

    @Test
    void invalidInputUsesExistingBadRequestEnvelope() {
        FeignException.BadRequest exception = assertThrows(FeignException.BadRequest.class,
                () -> context.getBean(LedgerClient.class).reverseJournalEntries(
                        new ReverseJournalEntriesRequest(List.of("captured-1"), " ")));
        assertTrue(exception.contentUTF8().contains("\"success\":false"));
        verifyNoInteractions(repository);
    }
}
