package io.asteria.balance.application.command;

import io.asteria.balance.domain.valueobject.BalanceAccountId;
import io.asteria.common.domain.valueobject.CurrencyCode;
import lombok.Builder;
import lombok.Getter;

/**
 * 扣除预留余额指令对象
 * */
@Getter
@Builder
public class ConsumeBalanceCommand {

    private final BalanceAccountId balanceAccountId;

    private final CurrencyCode currency;

    private final String referenceType;

    private final String referenceId;

    private final String eventId;
}