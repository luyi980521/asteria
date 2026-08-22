package io.asteria.ledger.application.service;

import io.asteria.ledger.application.command.CreateLedgerAccountCommand;
import io.asteria.ledger.domain.valueobject.LedgerAccountId;

/**
 * 账户功能接口定义
 * */
public interface LedgerAccountApplicationService {

    /**
     * 创建账户
     * @param command {@link CreateLedgerAccountCommand}
     * */
    LedgerAccountId createLedgerAccount(CreateLedgerAccountCommand command);
}
