package io.asteria.balance.application.command;

import io.asteria.balance.domain.valueobject.BalanceAccountId;
import io.asteria.common.domain.valueobject.CurrencyCode;
import lombok.Builder;
import lombok.Getter;

/**
 * 开户指令对象
 * */
@Getter
@Builder
public class CreateBalanceCommand {

    private final BalanceAccountId balanceAccountId;

    private final CurrencyCode currency;
}