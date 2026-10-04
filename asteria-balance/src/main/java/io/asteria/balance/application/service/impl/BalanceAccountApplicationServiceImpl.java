package io.asteria.balance.application.service.impl;

import io.asteria.balance.application.service.BalanceAccountApplicationService;
import io.asteria.balance.domain.entity.BalanceAccount;
import io.asteria.balance.domain.enums.BalanceAccountOwnerType;
import io.asteria.balance.domain.error.BalanceErrorCode;
import io.asteria.balance.domain.exception.BalanceDomainException;
import io.asteria.balance.domain.repository.BalanceAccountRepository;
import io.asteria.balance.domain.valueobject.BalanceAccountId;
import io.asteria.common.application.port.DistributedIdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BalanceAccountApplicationServiceImpl implements BalanceAccountApplicationService {
    private final BalanceAccountRepository repository;
    private final DistributedIdGenerator distributedIdGenerator;

    /**
     * 按所属主体创建账户；已存在时返回原账户ID。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public BalanceAccountId create(BalanceAccountOwnerType ownerType, Long ownerId) {
        if (ownerType == null || ownerId == null || ownerId <= 0) {
            throw new BalanceDomainException(BalanceErrorCode.INVALID_PARAMS);
        }
        var existing = repository.findByOwner(ownerType, ownerId);
        if (existing.isPresent()) {
            return existing.get().getBalanceAccountId();
        }
        BalanceAccount account = BalanceAccount.create(
                BalanceAccountId.of(distributedIdGenerator.nextId()), ownerType, ownerId);
        repository.save(account);
        return account.getBalanceAccountId();
    }
}
