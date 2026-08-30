package io.asteria.ledger.application.service;

import io.asteria.ledger.application.command.CreateLedgerAccountCommand;
import io.asteria.ledger.application.command.PostingCommand;
import io.asteria.ledger.domain.valueobject.LedgerAccountId;

import java.util.List;

/**
 * 账户功能接口定义
 * */
public interface LedgerAccountApplicationService {

    /**
     * 创建账户
     * @param command {@link CreateLedgerAccountCommand}
     * */
    LedgerAccountId createLedgerAccount(CreateLedgerAccountCommand command);

    /**
     * 校验分录账户是否合法
     * @param postingCommands {@link PostingCommand}s
     * */
    void validatePostable(List<PostingCommand> postingCommands);
}
