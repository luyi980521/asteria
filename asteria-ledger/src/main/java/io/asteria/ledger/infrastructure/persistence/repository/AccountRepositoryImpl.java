package io.asteria.ledger.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import io.asteria.ledger.domain.entity.Account;
import io.asteria.ledger.domain.error.LedgerErrorCode;
import io.asteria.ledger.domain.exception.LedgerDomainException;
import io.asteria.ledger.domain.repository.AccountRepository;
import io.asteria.ledger.domain.valueobject.AccountId;
import io.asteria.ledger.infrastructure.persistence.converter.AccountPersistenceConverter;
import io.asteria.ledger.infrastructure.persistence.dataobject.AccountDO;
import io.asteria.ledger.infrastructure.persistence.mapper.AccountMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
@RequiredArgsConstructor
public class AccountRepositoryImpl implements AccountRepository {

    private final AccountMapper accountMapper;
    private final AccountPersistenceConverter converter;

    @Override
    public void insert(Account account) {

        log.info("Inserting ledger account, ledgerAccountId={}, accountCode={}",
                account.getAccountId().value(), account.getAccountCode());

        accountMapper.insert(converter.toDataObject(account));
    }

    @Override
    public void updateByAccountCode(Account account) {

        log.info("Updating ledger account, accountCode: {}", account.getAccountCode());
        QueryWrapper<AccountDO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("account_code", account.getAccountCode());
        AccountDO oldAccountDO = accountMapper.selectOne(queryWrapper);
        if (oldAccountDO == null) {
            log.warn("Account doesn't exist: {}", account.getAccountCode());
            throw new LedgerDomainException(LedgerErrorCode.INVALID_PARAMS);
        }

        AccountDO newAccountDO = new AccountDO();
        newAccountDO.setAccountCode(account.getAccountCode());
        newAccountDO.setStatus(account.getStatus().name());
        UpdateWrapper<AccountDO> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("account_code", account.getAccountCode());

        accountMapper.update(newAccountDO, updateWrapper);
    }

    @Override
    public Account findById(AccountId accountId) {

        AccountDO dataObject = accountMapper.selectById(accountId.value());

        if (dataObject == null) {
            log.warn("Account doesn't exist: {}", accountId.value());;
            throw new LedgerDomainException(LedgerErrorCode.INVALID_PARAMS);
        }

        return converter.toDomain(dataObject);
    }

    @Override
    public Account findByAccountCode(String accountCode) {

        QueryWrapper<AccountDO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("account_code", accountCode);
        AccountDO oldAccountDO = accountMapper.selectOne(queryWrapper);
        return converter.toDomain(oldAccountDO);
    }

    @Override
    public boolean existsByAccountCode(String accountCode) {

        QueryWrapper<AccountDO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("account_code", accountCode);
        AccountDO oldAccountDO = accountMapper.selectOne(queryWrapper);
        return oldAccountDO != null;
    }
}