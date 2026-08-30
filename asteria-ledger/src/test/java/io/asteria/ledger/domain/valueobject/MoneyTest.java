package io.asteria.ledger.domain.valueobject;

import io.asteria.common.domain.error.CommonErrorCode;
import io.asteria.common.domain.exception.CommonDomainException;
import io.asteria.common.domain.valueobject.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MoneyTest {

    private static final Currency USD = Currency.getInstance("USD");
    private static final Currency JPY = Currency.getInstance("JPY");
    private static final Currency EUR = Currency.getInstance("EUR");

    @Test
    void canCreateValidUsdMoney() {
        Money money = Money.of(new BigDecimal("10.25"), USD);

        assertEquals(new BigDecimal("10.25"), money.amount());
        assertEquals(USD, money.currency());
    }

    @Test
    void canCreateValidJpyMoney() {
        Money money = Money.of(new BigDecimal("100"), JPY);

        assertEquals(new BigDecimal("100"), money.amount());
        assertEquals(JPY, money.currency());
    }

    @Test
    void amountNullReturnsNullArgument() {
        CommonDomainException exception = assertThrows(CommonDomainException.class, () -> Money.of(null, USD));

        assertEquals(CommonErrorCode.MONEY_AMOUNT_REQUIRED, exception.errorCode());
    }

    @Test
    void currencyNullReturnsNullArgument() {
        CommonDomainException exception = assertThrows(CommonDomainException.class, () -> Money.of(new BigDecimal("1.00"), null));

        assertEquals(CommonErrorCode.MONEY_CURRENCY_REQUIRED, exception.errorCode());
    }

    @Test
    void zeroAmountReturnsInvalidMoneyAmount() {
        CommonDomainException exception = assertThrows(CommonDomainException.class, () -> Money.of(BigDecimal.ZERO, USD));

        assertEquals(CommonErrorCode.MONEY_AMOUNT_MUST_BE_POSITIVE, exception.errorCode());
    }

    @Test
    void negativeAmountReturnsInvalidMoneyAmount() {
        CommonDomainException exception = assertThrows(CommonDomainException.class, () -> Money.of(new BigDecimal("-1"), USD));

        assertEquals(CommonErrorCode.MONEY_AMOUNT_MUST_BE_POSITIVE, exception.errorCode());
    }

    @Test
    void excessivePrecisionReturnsInvalidMoneyAmount() {
        CommonDomainException exception = assertThrows(CommonDomainException.class, () -> Money.of(new BigDecimal("10.257"), USD));

        assertEquals(CommonErrorCode.MONEY_AMOUNT_SCALE_INVALID, exception.errorCode());
    }

    @Test
    void sameCurrencyMoneyCanBeAdded() {
        Money original = Money.of(new BigDecimal("10.25"), USD);
        Money result = original.add(Money.of(new BigDecimal("2.75"), USD));

        assertEquals(Money.of(new BigDecimal("13.00"), USD), result);
    }

    @Test
    void addDoesNotModifyOriginalObject() {
        Money original = Money.of(new BigDecimal("10.25"), USD);

        original.add(Money.of(new BigDecimal("2.75"), USD));

        assertEquals(Money.of(new BigDecimal("10.25"), USD), original);
    }

    @Test
    void differentCurrencyAdditionReturnsCurrencyMismatch() {
        Money usd = Money.of(new BigDecimal("10.25"), USD);
        Money eur = Money.of(new BigDecimal("2.75"), EUR);

        CommonDomainException exception = assertThrows(CommonDomainException.class, () -> usd.add(eur));

        assertEquals(CommonErrorCode.MONEY_CURRENCY_MISMATCH, exception.errorCode());
    }

    @Test
    void addNullReturnsNullArgument() {
        Money money = Money.of(new BigDecimal("10.25"), USD);

        CommonDomainException exception = assertThrows(CommonDomainException.class, () -> money.add(null));

        assertEquals(CommonErrorCode.MONEY_OTHER_REQUIRED, exception.errorCode());
    }

    @Test
    void equalMoneyValuesAreEqual() {
        assertEquals(Money.of(new BigDecimal("10.25"), USD), Money.of(new BigDecimal("10.250"), USD));
    }

    @Test
    void differentCurrencyMoneyAreNotEqual() {
        assertNotEquals(Money.of(new BigDecimal("10.25"), USD), Money.of(new BigDecimal("10.25"), EUR));
    }
}
