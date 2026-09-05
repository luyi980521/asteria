package io.asteria.settlement.application.publisher;

import io.asteria.settlement.domain.repository.SettlementOutboxRepository;
import io.asteria.settlement.domain.valueobject.SettlementOutboxEvent;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.*;

class SettlementOutboxPublisherTest {

    private final SettlementOutboxRepository repository = mock(SettlementOutboxRepository.class);
    private final SettlementMessagePublisher messagePublisher = mock(SettlementMessagePublisher.class);
    private final SettlementOutboxPublisher publisher = new SettlementOutboxPublisher(repository, messagePublisher);

    @Test
    void continuesBatchAfterOneSubmissionFails() {
        SettlementOutboxEvent first = SettlementOutboxEvent.builder().id(1L).build();
        SettlementOutboxEvent second = SettlementOutboxEvent.builder().id(2L).build();
        when(repository.findPending(100)).thenReturn(List.of(first, second));
        doThrow(new IllegalStateException("Send rejected")).when(messagePublisher).publish(first);

        publisher.publishPendingEvents();

        verify(messagePublisher).publish(first);
        verify(messagePublisher).publish(second);
        verify(repository).findPending(100);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void emptyPollDoesNotPublish() {
        when(repository.findPending(100)).thenReturn(List.of());
        publisher.publishPendingEvents();
        verifyNoInteractions(messagePublisher);
    }
}
