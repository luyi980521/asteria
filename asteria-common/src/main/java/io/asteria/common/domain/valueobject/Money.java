package io.asteria.common.domain.valueobject;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.asteria.common.domain.error.CommonErrorCode;
import io.asteria.common.domain.exception.CommonDomainException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public final class Money {

    private final BigDecimal amount;
    private final CurrencyCode currency;

    private Money(BigDecimal amount, CurrencyCode currency) {
        this.currency = requireCurrency(currency);
        this.amount = normalize(requirePositiveAmount(amount), this.currency);
    }

    @JsonCreator
    public static Money of(@JsonProperty("amount") BigDecimal amount,
                           @JsonProperty("currency") CurrencyCode currency) {
        return new Money(amount, currency);
    }

    @JsonProperty("amount")
    public BigDecimal amount() {
        return amount;
    }

    @JsonProperty("currency")
    public CurrencyCode currency() {
        return currency;
    }

    public Money add(Money other) {
        requireOther(other);
        if (!currency.equals(other.currency)) {
            throw new CommonDomainException(CommonErrorCode.MONEY_CURRENCY_MISMATCH);
        }
        return new Money(amount.add(other.amount), currency);
    }

    private static BigDecimal requirePositiveAmount(BigDecimal amount) {
        if (amount == null) {
            throw new CommonDomainException(CommonErrorCode.MONEY_AMOUNT_REQUIRED);
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CommonDomainException(CommonErrorCode.MONEY_AMOUNT_MUST_BE_POSITIVE);
        }
        return amount;
    }

    private static CurrencyCode requireCurrency(CurrencyCode currency) {
        if (currency == null) {
            throw new CommonDomainException(CommonErrorCode.MONEY_CURRENCY_REQUIRED);
        }
        return currency;
    }

    private static void requireOther(Money other) {
        if (other == null) {
            throw new CommonDomainException(CommonErrorCode.MONEY_OTHER_REQUIRED);
        }
    }

    private static BigDecimal normalize(BigDecimal amount, CurrencyCode currency) {
        int fractionDigits = MoneyCurrencyScale.fractionDigits(currency);
        if (fractionDigits < 0) {
            throw new CommonDomainException(CommonErrorCode.MONEY_AMOUNT_SCALE_INVALID);
        }

        try {
            return amount.setScale(fractionDigits, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException ex) {
            throw new CommonDomainException(CommonErrorCode.MONEY_AMOUNT_SCALE_INVALID, ex);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Money money)) {
            return false;
        }
        return amount.equals(money.amount) && currency.equals(money.currency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount, currency);
    }

    @Override
    public String toString() {
        return amount + " " + currency.value();
    }
}
