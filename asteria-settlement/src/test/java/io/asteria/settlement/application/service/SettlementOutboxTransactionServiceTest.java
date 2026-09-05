package io.asteria.settlement.application.service;

import io.asteria.settlement.application.service.impl.SettlementOutboxTransactionServiceImpl;
import io.asteria.settlement.domain.repository.SettlementOutboxRepository;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SettlementOutboxTransactionServiceTest {

    private final SettlementOutboxRepository repository = mock(SettlementOutboxRepository.class);
    private final PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
    private final TransactionStatus transactionStatus = mock(TransactionStatus.class);

    private SettlementOutboxTransactionService transactionalService() {
        when(transactionManager.getTransaction(any(TransactionDefinition.class))).thenReturn(transactionStatus);
        TransactionInterceptor interceptor = new TransactionInterceptor();
        interceptor.setTransactionManager(transactionManager);
        interceptor.setTransactionAttributeSource(new AnnotationTransactionAttributeSource());
        ProxyFactory factory = new ProxyFactory(new SettlementOutboxTransactionServiceImpl(repository));
        factory.addAdvice(interceptor);
        return (SettlementOutboxTransactionService) factory.getProxy();
    }

    @Test
    void callbackUpdateRunsInsideTransactionAndCommits() {
        SettlementOutboxTransactionService service = transactionalService();
        Instant publishedAt = Instant.now();
        service.markPublished(1L, publishedAt);

        var order = inOrder(transactionManager, repository);
        order.verify(transactionManager).getTransaction(any(TransactionDefinition.class));
        order.verify(repository).markPublished(1L, publishedAt);
        order.verify(transactionManager).commit(transactionStatus);
    }

    @Test
    void databaseFailureRollsBackCallbackTransaction() {
        SettlementOutboxTransactionService service = transactionalService();
        Instant publishedAt = Instant.now();
        doThrow(new IllegalStateException("Database unavailable")).when(repository).markPublished(1L, publishedAt);

        assertThrows(IllegalStateException.class, () -> service.markPublished(1L, publishedAt));

        verify(transactionManager).rollback(transactionStatus);
        verify(transactionManager, never()).commit(any());
    }
}
