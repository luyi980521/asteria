package io.asteria.balance.application.service;

import io.asteria.balance.application.command.ConsumeBalanceCommand;
import io.asteria.balance.application.command.CreditBalanceCommand;
import io.asteria.balance.application.command.ReleaseBalanceCommand;
import io.asteria.balance.application.command.ReserveBalanceCommand;

/**
 * 余额功能接口定义
 * */
public interface BalanceApplicationService {

    /**
     * 增加可用余额。
     */
    void credit(CreditBalanceCommand command);

    /**
     * 预留余额：将可用余额转入预留余额。
     */
    void reserve(ReserveBalanceCommand command);

    /**
     * 释放预留余额：将预留余额退回可用余额。
     */
    void release(ReleaseBalanceCommand command);

    /**
     * 扣除预留余额：将预留金额正式从余额中扣除。
     */
    void consume(ConsumeBalanceCommand command);
}
