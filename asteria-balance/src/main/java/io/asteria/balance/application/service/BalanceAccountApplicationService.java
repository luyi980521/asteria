package io.asteria.balance.application.service;

import io.asteria.balance.domain.enums.BalanceAccountOwnerType;
import io.asteria.balance.domain.valueobject.BalanceAccountId;

public interface BalanceAccountApplicationService {

    /**
     * 按所属主体创建账户；已存在时返回原账户ID。
     * */
    BalanceAccountId create(BalanceAccountOwnerType ownerType, Long ownerId);
}
