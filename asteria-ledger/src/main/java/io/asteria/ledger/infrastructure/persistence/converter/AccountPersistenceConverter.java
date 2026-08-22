package io.asteria.ledger.infrastructure.persistence.converter;

import io.asteria.ledger.domain.entity.Account;
import io.asteria.ledger.domain.enums.AccountCategory;
import io.asteria.ledger.domain.enums.AccountOwnerType;
import io.asteria.ledger.domain.enums.AccountStatus;
import io.asteria.ledger.domain.valueobject.AccountId;
import io.asteria.ledger.infrastructure.persistence.dataobject.AccountDO;
import org.springframework.stereotype.Component;

import java.util.Currency;

@Component
public class AccountPersistenceConverter {

    public AccountDO toDataObject(Account account) {
        AccountDO dataObject = new AccountDO();

        dataObject.setId(account.getAccountId().value());
        dataObject.setAccountCode(account.getAccountCode());
        dataObject.setOwnerType(account.getOwnerType().name());
        dataObject.setOwnerId(account.getOwnerId());
        dataObject.setCategory(account.getCategory().name());
        dataObject.setCurrency(account.getCurrency().getCurrencyCode());
        dataObject.setStatus(account.getStatus().name());
        dataObject.setAllowNegativeBalance(account.isAllowNegativeBalance());

        return dataObject;
    }

    public Account toDomain(AccountDO dataObject) {
        return Account.reconstitute(
                AccountId.of(dataObject.getId()),
                dataObject.getAccountCode(),
                AccountOwnerType.valueOf(dataObject.getOwnerType()),
                dataObject.getOwnerId(),
                AccountCategory.valueOf(dataObject.getCategory()),
                Currency.getInstance(dataObject.getCurrency()),
                AccountStatus.valueOf(dataObject.getStatus()),
                Boolean.TRUE.equals(dataObject.getAllowNegativeBalance())
        );
    }
}