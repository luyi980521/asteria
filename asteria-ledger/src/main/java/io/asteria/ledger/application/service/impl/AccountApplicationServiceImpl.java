package io.asteria.ledger.application.service.impl;

import io.asteria.common.application.port.DistributedIdGenerator;
import io.asteria.ledger.application.command.CreateAccountCommand;
import io.asteria.ledger.application.service.AccountApplicationService;
import io.asteria.ledger.domain.entity.Account;
import io.asteria.ledger.domain.error.LedgerErrorCode;
import io.asteria.ledger.domain.exception.LedgerDomainException;
import io.asteria.ledger.domain.repository.AccountRepository;
import io.asteria.ledger.domain.valueobject.AccountId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 账户功能接口定义实现类
 * */
@Slf4j
@Service
public class AccountApplicationServiceImpl implements AccountApplicationService {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private DistributedIdGenerator distributedIdGenerator;

    /**
     * 创建账户
     *
     * @param command {@link CreateAccountCommand}
     */
    @Override
    public AccountId create(CreateAccountCommand command) {

        boolean isExist = accountRepository.existsByAccountCode(command.accountCode());
        if (!isExist) {
            log.warn("Ledger account already exists, accountCode: {}", command.accountCode());
            throw new LedgerDomainException(LedgerErrorCode.LEDGER_ACCOUNT_ALREADY_EXISTS);
        }

        AccountId accountId = AccountId.of(distributedIdGenerator.nextId());
        Account account = Account.create(
                accountId,
                command.accountCode(),
                command.ownerType(),
                command.ownerId(),
                command.category(),
                command.currency(),
                command.allowNegativeBalance()
        );

        accountRepository.insert(account);
        log.info("Ledger account created successfully, accountId: {}, accountCode: {}",
                accountId.value(), command.accountCode());
        return accountId;
    }
}
