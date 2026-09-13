package io.asteria.balance.domain.entity;

import io.asteria.balance.domain.error.BalanceErrorCode;
import io.asteria.balance.domain.exception.BalanceDomainException;
import io.asteria.balance.domain.valueobject.BalanceAccountId;
import io.asteria.balance.domain.valueobject.BalanceId;
import io.asteria.common.domain.valueobject.CurrencyCode;
import io.asteria.common.domain.valueobject.Money;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

/**
 * 单个余额账户在指定币种下的余额；持久化版本递增由后续 Repository 负责。
 * */
@Getter
public final class Balance {

    /** 余额标识 */
    private final BalanceId balanceId;

    /** 余额账户标识 */
    private final BalanceAccountId balanceAccountId;

    /** 余额币种 */
    private final CurrencyCode currency;

    /** 可用金额 */
    private Money availableAmount;

    /** 预留金额 */
    private Money reservedAmount;

    /** 持久化乐观锁版本，领域操作不自行递增 */
    private final Long version;

    /** 创建时间 */
    private final Instant createdAt;

    /** 最后变更时间 */
    private Instant updatedAt;

    @Builder
    private Balance(BalanceId balanceId, BalanceAccountId balanceAccountId, CurrencyCode currency,
                    Money availableAmount, Money reservedAmount, Long version,
                    Instant createdAt, Instant updatedAt) {
        if (balanceId == null || balanceAccountId == null || currency == null
                || createdAt == null || updatedAt == null) {
            throw new BalanceDomainException(BalanceErrorCode.INVALID_PARAMS);
        }
        if (availableAmount == null || reservedAmount == null || version == null || version < 0
                || availableAmount.amount().signum() < 0 || reservedAmount.amount().signum() < 0) {
            throw new BalanceDomainException(BalanceErrorCode.INVALID_BALANCE_STATE);
        }
        if (!currency.equals(availableAmount.currency()) || !currency.equals(reservedAmount.currency())) {
            throw new BalanceDomainException(BalanceErrorCode.CURRENCY_MISMATCH);
        }
        this.balanceId = balanceId;
        this.balanceAccountId = balanceAccountId;
        this.currency = currency;
        this.availableAmount = availableAmount;
        this.reservedAmount = reservedAmount;
        this.version = version;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /** 入账至可用余额。 */
    public void credit(Money amount) {
        requireAmount(amount);
        availableAmount = Money.ofNonNegative(availableAmount.amount().add(amount.amount()), currency);
        updatedAt = Instant.now();
    }

    /** 将可用金额转入预留金额。 */
    public void reserve(Money amount) {
        requireAmount(amount);
        if (amount.amount().compareTo(availableAmount.amount()) > 0) {
            throw new BalanceDomainException(BalanceErrorCode.INSUFFICIENT_AVAILABLE_BALANCE);
        }
        Money newAvailable = Money.ofNonNegative(availableAmount.amount().subtract(amount.amount()), currency);
        Money newReserved = Money.ofNonNegative(reservedAmount.amount().add(amount.amount()), currency);
        availableAmount = newAvailable;
        reservedAmount = newReserved;
        updatedAt = Instant.now();
    }

    /** 将预留金额释放回可用金额。 */
    public void release(Money amount) {
        requireReservedAmount(amount);
        Money newAvailable = Money.ofNonNegative(availableAmount.amount().add(amount.amount()), currency);
        Money newReserved = Money.ofNonNegative(reservedAmount.amount().subtract(amount.amount()), currency);
        availableAmount = newAvailable;
        reservedAmount = newReserved;
        updatedAt = Instant.now();
    }

    /** 扣减预留金额，不增加可用金额。 */
    public void debitReserved(Money amount) {
        requireReservedAmount(amount);
        reservedAmount = Money.ofNonNegative(reservedAmount.amount().subtract(amount.amount()), currency);
        updatedAt = Instant.now();
    }

    private void requireAmount(Money amount) {
        if (amount == null || amount.amount().signum() <= 0) {
            throw new BalanceDomainException(BalanceErrorCode.INVALID_AMOUNT);
        }
        if (!currency.equals(amount.currency())) {
            throw new BalanceDomainException(BalanceErrorCode.CURRENCY_MISMATCH);
        }
    }

    private void requireReservedAmount(Money amount) {
        requireAmount(amount);
        if (amount.amount().compareTo(reservedAmount.amount()) > 0) {
            throw new BalanceDomainException(BalanceErrorCode.INSUFFICIENT_RESERVED_BALANCE);
        }
    }
}
