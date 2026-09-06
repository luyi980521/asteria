package io.asteria.ledger.infrastructure.persistence.converter;

import io.asteria.common.domain.valueobject.CurrencyCode;
import io.asteria.ledger.domain.entity.LedgerAccount;
import io.asteria.ledger.domain.enums.LedgerAccountCategory;
import io.asteria.ledger.domain.enums.LedgerAccountOwnerType;
import io.asteria.ledger.domain.enums.LedgerAccountStatus;
import io.asteria.ledger.domain.valueobject.LedgerAccountId;
import io.asteria.ledger.infrastructure.persistence.dataobject.LedgerAccountDO;
import org.springframework.stereotype.Component;


@Component
public class LedgerAccountPersistenceConverter {

    public LedgerAccountDO toDataObject(LedgerAccount ledgerAccount) {
        LedgerAccountDO dataObject = new LedgerAccountDO();

        dataObject.setId(ledgerAccount.getLedgerAccountId().value());
        dataObject.setAccountCode(ledgerAccount.getAccountCode());
        dataObject.setOwnerType(ledgerAccount.getOwnerType().name());
        dataObject.setOwnerId(ledgerAccount.getOwnerId());
        dataObject.setCategory(ledgerAccount.getCategory().name());
        dataObject.setCurrency(ledgerAccount.getCurrency().value());
        dataObject.setStatus(ledgerAccount.getStatus().name());
        dataObject.setAllowNegativeBalance(ledgerAccount.isAllowNegativeBalance());

        return dataObject;
    }

    public LedgerAccount toDomain(LedgerAccountDO dataObject) {
        return LedgerAccount.reconstitute(
                LedgerAccountId.of(dataObject.getId()),
                dataObject.getAccountCode(),
                LedgerAccountOwnerType.valueOf(dataObject.getOwnerType()),
                dataObject.getOwnerId(),
                LedgerAccountCategory.valueOf(dataObject.getCategory()),
                CurrencyCode.of(dataObject.getCurrency()),
                LedgerAccountStatus.valueOf(dataObject.getStatus()),
                Boolean.TRUE.equals(dataObject.getAllowNegativeBalance())
        );
    }
}
