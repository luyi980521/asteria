package io.asteria.ledger.application.service.impl;

import io.asteria.common.application.port.DistributedIdGenerator;
import io.asteria.ledger.application.command.CreateLedgerAccountCommand;
import io.asteria.ledger.application.service.LedgerAccountApplicationService;
import io.asteria.ledger.domain.entity.LedgerAccount;
import io.asteria.ledger.domain.error.LedgerErrorCode;
import io.asteria.ledger.domain.exception.LedgerDomainException;
import io.asteria.ledger.domain.repository.LedgerAccountRepository;
import io.asteria.ledger.domain.valueobject.LedgerAccountId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 账户功能接口定义实现类
 * */
@Slf4j
@Service
public class LedgerAccountApplicationServiceImpl implements LedgerAccountApplicationService {

    @Autowired
    private LedgerAccountRepository ledgerAccountRepository;

    @Autowired
    private DistributedIdGenerator distributedIdGenerator;

    /**
     * 创建账户
     *
     * @param command {@link CreateLedgerAccountCommand}
     */
    @Override
    public LedgerAccountId createLedgerAccount(CreateLedgerAccountCommand command) {

        boolean isExist = ledgerAccountRepository.existsByAccountCode(command.accountCode());
        if (!isExist) {
            log.warn("Ledger account already exists, accountCode: {}", command.accountCode());
            throw new LedgerDomainException(LedgerErrorCode.LEDGER_ACCOUNT_ALREADY_EXISTS);
        }

        LedgerAccountId ledgerAccountId = LedgerAccountId.of(distributedIdGenerator.nextId());
        LedgerAccount ledgerAccount = LedgerAccount.create(
                ledgerAccountId,
                command.accountCode(),
                command.ownerType(),
                command.ownerId(),
                command.category(),
                command.currency(),
                command.allowNegativeBalance()
        );

        ledgerAccountRepository.insert(ledgerAccount);
        log.info("Ledger account created successfully, ledgerAccountId: {}, accountCode: {}",
                ledgerAccountId.value(), command.accountCode());
        return ledgerAccountId;
    }
}
