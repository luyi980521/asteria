package io.asteria.balance.infrastructure.persistence.converter;

import io.asteria.balance.domain.entity.Balance;
import io.asteria.balance.domain.valueobject.BalanceAccountId;
import io.asteria.balance.domain.valueobject.BalanceId;
import io.asteria.balance.infrastructure.persistence.dataobject.BalanceDO;
import io.asteria.common.domain.valueobject.CurrencyCode;
import io.asteria.common.domain.valueobject.Money;
import org.springframework.stereotype.Component;

@Component
public class BalancePersistenceConverter {
    public BalanceDO toDataObject(Balance balance) {
        return BalanceDO.builder()
                .id(balance.getBalanceId().value())
                .balanceAccountId(balance.getBalanceAccountId().value())
                .currency(balance.getCurrency().value())
                .availableAmount(balance.getAvailableAmount().amount())
                .reservedAmount(balance.getReservedAmount().amount())
                .version(balance.getVersion())
                .createdAt(balance.getCreatedAt())
                .updatedAt(balance.getUpdatedAt())
                .build();
    }

    public Balance toDomain(BalanceDO dataObject) {
        CurrencyCode currency = CurrencyCode.of(dataObject.getCurrency());
        return Balance.builder()
                .balanceId(BalanceId.of(dataObject.getId()))
                .balanceAccountId(BalanceAccountId.of(dataObject.getBalanceAccountId()))
                .currency(currency)
                .availableAmount(Money.ofNonNegative(dataObject.getAvailableAmount(), currency))
                .reservedAmount(Money.ofNonNegative(dataObject.getReservedAmount(), currency))
                .version(dataObject.getVersion())
                .createdAt(dataObject.getCreatedAt())
                .updatedAt(dataObject.getUpdatedAt())
                .build();
    }
}
