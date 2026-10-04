package io.asteria.balance.application.service.impl;

import io.asteria.balance.domain.entity.BalanceAccount;
import io.asteria.balance.domain.enums.BalanceAccountOwnerType;
import io.asteria.balance.domain.enums.BalanceAccountStatus;
import io.asteria.balance.domain.error.BalanceErrorCode;
import io.asteria.balance.domain.exception.BalanceDomainException;
import io.asteria.balance.domain.repository.BalanceAccountRepository;
import io.asteria.balance.domain.valueobject.BalanceAccountId;
import io.asteria.common.application.port.DistributedIdGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BalanceAccountApplicationServiceImplTest {
    private static final BalanceAccountId ACCOUNT_ID = BalanceAccountId.of(1L);
    private static final Long OWNER_ID = 2L;

    @Mock
    private BalanceAccountRepository repository;
    @Mock
    private DistributedIdGenerator distributedIdGenerator;
    @InjectMocks
    private BalanceAccountApplicationServiceImpl service;
    @Captor
    private ArgumentCaptor<BalanceAccount> accountCaptor;

    @ParameterizedTest
    @EnumSource(BalanceAccountOwnerType.class)
    void create_new_owner_should_save_active_account(BalanceAccountOwnerType ownerType) {
        when(repository.findByOwner(ownerType, OWNER_ID)).thenReturn(Optional.empty());
        when(distributedIdGenerator.nextId()).thenReturn(ACCOUNT_ID.value());

        BalanceAccountId result = service.create(ownerType, OWNER_ID);

        assertEquals(ACCOUNT_ID, result);
        verify(repository).save(accountCaptor.capture());
        BalanceAccount saved = accountCaptor.getValue();
        assertEquals(ACCOUNT_ID, saved.getBalanceAccountId());
        assertEquals(ownerType, saved.getOwnerType());
        assertEquals(OWNER_ID, saved.getOwnerId());
        assertEquals(BalanceAccountStatus.ACTIVE, saved.getStatus());
        assertNotNull(saved.getCreatedAt());
        assertEquals(saved.getCreatedAt(), saved.getUpdatedAt());
        verify(repository).findByOwner(ownerType, OWNER_ID);
        verify(distributedIdGenerator).nextId();
    }

    @ParameterizedTest
    @EnumSource(BalanceAccountStatus.class)
    void create_existing_owner_should_return_original_id_without_changing_status(BalanceAccountStatus status) {
        Instant created = Instant.parse("2026-01-01T00:00:00Z");
        BalanceAccount existing = BalanceAccount.builder().balanceAccountId(ACCOUNT_ID)
                .ownerType(BalanceAccountOwnerType.MERCHANT).ownerId(OWNER_ID).status(status)
                .createdAt(created).updatedAt(created).build();
        when(repository.findByOwner(BalanceAccountOwnerType.MERCHANT, OWNER_ID)).thenReturn(Optional.of(existing));

        assertEquals(ACCOUNT_ID, service.create(BalanceAccountOwnerType.MERCHANT, OWNER_ID));

        assertEquals(status, existing.getStatus());
        assertEquals(created, existing.getUpdatedAt());
        verify(repository, never()).save(any());
        verifyNoInteractions(distributedIdGenerator);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(longs = {0L, -1L})
    void create_invalid_owner_id_should_fail_before_query_or_id_generation(Long ownerId) {
        BalanceDomainException exception = assertThrows(BalanceDomainException.class,
                () -> service.create(BalanceAccountOwnerType.MERCHANT, ownerId));

        assertEquals(BalanceErrorCode.INVALID_PARAMS, exception.errorCode());
        verifyNoInteractions(repository, distributedIdGenerator);
    }

    @Test
    void create_null_owner_type_should_fail_before_query_or_id_generation() {
        BalanceDomainException exception = assertThrows(BalanceDomainException.class,
                () -> service.create(null, OWNER_ID));

        assertEquals(BalanceErrorCode.INVALID_PARAMS, exception.errorCode());
        verifyNoInteractions(repository, distributedIdGenerator);
    }
}
