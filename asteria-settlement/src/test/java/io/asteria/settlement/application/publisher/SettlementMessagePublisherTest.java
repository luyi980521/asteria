package io.asteria.settlement.application.publisher;

import io.asteria.common.constants.SettlementTopics;
import io.asteria.settlement.application.service.SettlementOutboxTransactionService;
import io.asteria.settlement.domain.enums.SettlementOutboxEventType;
import io.asteria.settlement.domain.exception.SettlementDomainException;
import io.asteria.settlement.domain.valueobject.SettlementOutboxEvent;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SettlementMessagePublisherTest {

    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, String> kafkaTemplate = mock(KafkaTemplate.class);
    private final SettlementOutboxTransactionService transactionService = mock(SettlementOutboxTransactionService.class);
    private final SettlementMessagePublisher publisher = new SettlementMessagePublisher(kafkaTemplate, transactionService);
    private final CompletableFuture<SendResult<String, String>> future = new CompletableFuture<>();
    private final SettlementOutboxEvent event = SettlementOutboxEvent.builder()
            .id(1L).eventId("event-1").aggregateId("42")
            .eventType(SettlementOutboxEventType.SETTLEMENT_COMPLETED)
            .payload("{\"eventId\":\"event-1\"}").build();

    @Test
    void sendsStoredPayloadAndMarksPublishedOnlyAfterAcknowledgement() {
        when(kafkaTemplate.send(SettlementTopics.SETTLEMENT_COMPLETED, "42", event.getPayload())).thenReturn(future);

        publisher.publish(event);

        verify(kafkaTemplate).send(SettlementTopics.SETTLEMENT_COMPLETED, "42", event.getPayload());
        verifyNoInteractions(transactionService);
        future.complete(null);
        verify(transactionService).markPublished(eq(1L), any(Instant.class));
    }

    @Test
    void asynchronousFailureDoesNotMarkPublished() {
        when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(future);
        publisher.publish(event);
        future.completeExceptionally(new IllegalStateException("Kafka unavailable"));
        verifyNoInteractions(transactionService);
    }

    @Test
    void synchronousFailureDoesNotMarkPublished() {
        when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenThrow(new IllegalStateException("Send rejected"));
        assertThrows(IllegalStateException.class, () -> publisher.publish(event));
        verifyNoInteractions(transactionService);
    }

    @Test
    void databaseFailureInCallbackIsContained() {
        when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(future);
        doThrow(new IllegalStateException("Database unavailable")).when(transactionService).markPublished(eq(1L), any());
        publisher.publish(event);
        assertDoesNotThrow(() -> future.complete(null));
        verify(transactionService).markPublished(eq(1L), any(Instant.class));
    }

    @Test
    void unsupportedTypeDoesNotSendOrMarkPublished() {
        SettlementOutboxEvent unsupported = SettlementOutboxEvent.builder().id(2L).build();
        assertThrows(SettlementDomainException.class, () -> publisher.publish(unsupported));
        verifyNoInteractions(kafkaTemplate, transactionService);
    }
}
