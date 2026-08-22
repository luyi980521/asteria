package io.asteria.ledger.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import io.asteria.ledger.domain.entity.LedgerAccount;
import io.asteria.ledger.domain.error.LedgerErrorCode;
import io.asteria.ledger.domain.exception.LedgerDomainException;
import io.asteria.ledger.domain.repository.LedgerAccountRepository;
import io.asteria.ledger.domain.valueobject.LedgerAccountId;
import io.asteria.ledger.infrastructure.persistence.converter.LedgerAccountPersistenceConverter;
import io.asteria.ledger.infrastructure.persistence.dataobject.LedgerAccountDO;
import io.asteria.ledger.infrastructure.persistence.mapper.LedgerAccountMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
@RequiredArgsConstructor
public class LedgerAccountRepositoryImpl implements LedgerAccountRepository {

    private final LedgerAccountMapper ledgerAccountMapper;
    private final LedgerAccountPersistenceConverter converter;

    @Override
    public void insert(LedgerAccount ledgerAccount) {

        log.info("Inserting ledger account, ledgerAccountId={}, accountCode={}",
                ledgerAccount.getLedgerAccountId().value(), ledgerAccount.getAccountCode());

        ledgerAccountMapper.insert(converter.toDataObject(ledgerAccount));
    }

    @Override
    public void updateByAccountCode(LedgerAccount ledgerAccount) {

        log.info("Updating ledger account, accountCode: {}", ledgerAccount.getAccountCode());
        QueryWrapper<LedgerAccountDO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("account_code", ledgerAccount.getAccountCode());
        LedgerAccountDO oldLedgerAccountDO = ledgerAccountMapper.selectOne(queryWrapper);
        if (oldLedgerAccountDO == null) {
            log.warn("Ledger account doesn't exist: {}", ledgerAccount.getAccountCode());
            throw new LedgerDomainException(LedgerErrorCode.INVALID_PARAMS);
        }

        LedgerAccountDO newLedgerAccountDO = new LedgerAccountDO();
        newLedgerAccountDO.setAccountCode(ledgerAccount.getAccountCode());
        newLedgerAccountDO.setStatus(ledgerAccount.getStatus().name());
        UpdateWrapper<LedgerAccountDO> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("account_code", ledgerAccount.getAccountCode());

        ledgerAccountMapper.update(newLedgerAccountDO, updateWrapper);
    }

    @Override
    public LedgerAccount findById(LedgerAccountId ledgerAccountId) {

        LedgerAccountDO dataObject = ledgerAccountMapper.selectById(ledgerAccountId.value());

        if (dataObject == null) {
            log.warn("Ledger account doesn't exist: {}", ledgerAccountId.value());;
            throw new LedgerDomainException(LedgerErrorCode.INVALID_PARAMS);
        }

        return converter.toDomain(dataObject);
    }

    @Override
    public LedgerAccount findByAccountCode(String accountCode) {

        QueryWrapper<LedgerAccountDO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("account_code", accountCode);
        LedgerAccountDO oldLedgerAccountDO = ledgerAccountMapper.selectOne(queryWrapper);
        return converter.toDomain(oldLedgerAccountDO);
    }

    @Override
    public boolean existsByAccountCode(String accountCode) {

        QueryWrapper<LedgerAccountDO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("account_code", accountCode);
        LedgerAccountDO oldLedgerAccountDO = ledgerAccountMapper.selectOne(queryWrapper);
        return oldLedgerAccountDO != null;
    }
}
