package io.asteria.ledger.application.service.impl;

import io.asteria.common.application.port.DistributedIdGenerator;
import io.asteria.ledger.application.command.CreateLedgerAccountCommand;
import io.asteria.ledger.application.command.PostingCommand;
import io.asteria.ledger.application.service.LedgerAccountApplicationService;
import io.asteria.ledger.domain.entity.LedgerAccount;
import io.asteria.ledger.domain.error.LedgerErrorCode;
import io.asteria.ledger.domain.exception.LedgerDomainException;
import io.asteria.ledger.domain.repository.LedgerAccountRepository;
import io.asteria.ledger.domain.valueobject.LedgerAccountId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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
        if (isExist) {
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

    /**
     * 校验分录账户是否合法
     *
     * @param postingCommands {@link PostingCommand}s
     */
    @Override
    public void validatePostable(List<PostingCommand> postingCommands) {

        List<LedgerAccountId> ledgerAccountIds = postingCommands.stream()
                .map(PostingCommand::ledgerAccountId)
                .distinct()
                .toList();
        List<LedgerAccount> ledgerAccounts = ledgerAccountRepository.findByLedgerAccountIds(ledgerAccountIds);
        // 校验去重后的参数数量和结果是否一致，如果不一致则表示缺少需要的账户
        if (ledgerAccountIds.size() != ledgerAccounts.size()) {
            log.warn("Ledger account id size doesn't equal ledger account size: {}, {}",
                    ledgerAccountIds.size(), ledgerAccounts.size());
            throw new LedgerDomainException(LedgerErrorCode.LEDGER_ACCOUNT_NOT_EXIST);
        }

        Map<LedgerAccountId, LedgerAccount> ledgerAccountIdLedgerAccountMap = ledgerAccounts.stream().
                collect(Collectors.toMap(LedgerAccount::getLedgerAccountId, Function.identity()));
        // 校验每个账户是否允许记账
        for (PostingCommand postingCommand : postingCommands) {
            LedgerAccount ledgerAccount = ledgerAccountIdLedgerAccountMap.get(postingCommand.ledgerAccountId());
            // 二次校验，避免查询层面的代码改了而出现NPE
            if (ledgerAccount == null) {
                log.warn("Ledger account does not exist, ledgerAccountId: {}", postingCommand.ledgerAccountId().value());
                throw new LedgerDomainException(LedgerErrorCode.LEDGER_ACCOUNT_NOT_EXIST);
            }
            ledgerAccount.validatePostable(postingCommand.money());
        }
    }
}
