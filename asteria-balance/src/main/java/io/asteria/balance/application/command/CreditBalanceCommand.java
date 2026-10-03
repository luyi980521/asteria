package io.asteria.balance.application.command;

import io.asteria.balance.domain.valueobject.BalanceAccountId;
import io.asteria.common.domain.valueobject.CurrencyCode;
import io.asteria.common.domain.valueobject.Money;
import lombok.Builder;
import lombok.Getter;

/**
 * 增加可用余额指令对象
 * */
@Getter
@Builder
public class CreditBalanceCommand {

    private final BalanceAccountId balanceAccountId;

    private final CurrencyCode currency;

    private final Money amount;

    /**
     * 业务来源，例如 PAYMENT、SETTLEMENT、REFUND。
     */
    private final String referenceType;

    /**
     * 业务对象 ID。
     */
    private final String referenceId;

    /**
     * 当前业务事件 ID，用于 Movement 幂等。
     */
    private final String eventId;
}