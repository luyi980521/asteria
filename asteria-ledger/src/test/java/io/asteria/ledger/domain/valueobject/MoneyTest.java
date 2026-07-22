package io.asteria.ledger.domain.valueobject;

import io.asteria.ledger.domain.error.LedgerErrorCode;
import io.asteria.ledger.domain.exception.LedgerDomainException;
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
        LedgerDomainException exception = assertThrows(LedgerDomainException.class, () -> Money.of(null, USD));

        assertEquals(LedgerErrorCode.NULL_ARGUMENT, exception.errorCode());
    }

    @Test
    void currencyNullReturnsNullArgument() {
        LedgerDomainException exception = assertThrows(LedgerDomainException.class, () -> Money.of(new BigDecimal("1.00"), null));

        assertEquals(LedgerErrorCode.NULL_ARGUMENT, exception.errorCode());
    }

    @Test
    void zeroAmountReturnsInvalidMoneyAmount() {
        LedgerDomainException exception = assertThrows(LedgerDomainException.class, () -> Money.of(BigDecimal.ZERO, USD));

        assertEquals(LedgerErrorCode.INVALID_MONEY_AMOUNT, exception.errorCode());
    }

    @Test
    void negativeAmountReturnsInvalidMoneyAmount() {
        LedgerDomainException exception = assertThrows(LedgerDomainException.class, () -> Money.of(new BigDecimal("-1"), USD));

        assertEquals(LedgerErrorCode.INVALID_MONEY_AMOUNT, exception.errorCode());
    }

    @Test
    void excessivePrecisionReturnsInvalidMoneyAmount() {
        LedgerDomainException exception = assertThrows(LedgerDomainException.class, () -> Money.of(new BigDecimal("10.257"), USD));

        assertEquals(LedgerErrorCode.INVALID_MONEY_AMOUNT, exception.errorCode());
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

        LedgerDomainException exception = assertThrows(LedgerDomainException.class, () -> usd.add(eur));

        assertEquals(LedgerErrorCode.CURRENCY_MISMATCH, exception.errorCode());
    }

    @Test
    void addNullReturnsNullArgument() {
        Money money = Money.of(new BigDecimal("10.25"), USD);

        LedgerDomainException exception = assertThrows(LedgerDomainException.class, () -> money.add(null));

        assertEquals(LedgerErrorCode.NULL_ARGUMENT, exception.errorCode());
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
