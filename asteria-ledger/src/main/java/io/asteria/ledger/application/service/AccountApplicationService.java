package io.asteria.ledger.application.service;

import io.asteria.ledger.application.command.CreateAccountCommand;
import io.asteria.ledger.domain.valueobject.AccountId;

/**
 * 账户功能接口定义
 * */
public interface AccountApplicationService {

    /**
     * 创建账户
     * @param command {@link CreateAccountCommand}
     * */
    AccountId create(CreateAccountCommand command);
}
