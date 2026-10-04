package io.asteria.balance.infrastructure.persistence.converter;

import io.asteria.balance.domain.entity.BalanceAccount;
import io.asteria.balance.domain.enums.BalanceAccountOwnerType;
import io.asteria.balance.domain.enums.BalanceAccountStatus;
import io.asteria.balance.domain.valueobject.BalanceAccountId;
import io.asteria.balance.infrastructure.persistence.dataobject.BalanceAccountDO;
import org.springframework.stereotype.Component;

@Component
public class BalanceAccountPersistenceConverter {
    public BalanceAccountDO toDataObject(BalanceAccount account) {
        return BalanceAccountDO.builder()
                .id(account.getBalanceAccountId().value())
                .ownerType(account.getOwnerType().name())
                .ownerId(account.getOwnerId())
                .status(account.getStatus().name())
                .createdAt(account.getCreatedAt())
                .updatedAt(account.getUpdatedAt())
                .build();
    }

    public BalanceAccount toDomain(BalanceAccountDO dataObject) {
        return BalanceAccount.builder()
                .balanceAccountId(BalanceAccountId.of(dataObject.getId()))
                .ownerType(BalanceAccountOwnerType.valueOf(dataObject.getOwnerType()))
                .ownerId(dataObject.getOwnerId())
                .status(BalanceAccountStatus.valueOf(dataObject.getStatus()))
                .createdAt(dataObject.getCreatedAt())
                .updatedAt(dataObject.getUpdatedAt())
                .build();
    }
}
