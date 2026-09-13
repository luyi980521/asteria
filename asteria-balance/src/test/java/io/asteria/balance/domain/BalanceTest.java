package io.asteria.balance.domain;

import io.asteria.balance.domain.entity.Balance;
import io.asteria.balance.domain.enums.BalanceMovementType;
import io.asteria.balance.domain.error.BalanceErrorCode;
import io.asteria.balance.domain.exception.BalanceDomainException;
import io.asteria.balance.domain.valueobject.BalanceAccountId;
import io.asteria.balance.domain.valueobject.BalanceId;
import io.asteria.common.domain.valueobject.CurrencyCode;
import io.asteria.common.domain.valueobject.Money;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

class BalanceTest {
    private static final CurrencyCode USD = CurrencyCode.of("USD");
    private static final Instant CREATED = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void creditIncreasesOnlyAvailableAndPreservesPersistenceVersion() {
        Balance balance = builder().build();
        Instant before = Instant.now();
        balance.credit(money("25"));
        assertAmounts(balance, "125", "20");
        assertEquals(3L, balance.getVersion());
        assertEquals(CREATED, balance.getCreatedAt());
        assertFalse(balance.getUpdatedAt().isBefore(before));
    }

    @Test
    void reserveAndReleaseAllowExactBalanceAndConserveTotal() {
        Balance balance = builder().build();
        balance.reserve(money("100"));
        assertAmounts(balance, "0", "120");
        balance.release(money("120"));
        assertAmounts(balance, "120", "0");
    }

    @Test
    void debitReservedCanConsumeAllWithoutCreditingAvailable() {
        Balance balance = builder().build();
        balance.debitReserved(money("20"));
        assertAmounts(balance, "100", "0");
    }

    @Test
    void canCreateAndCreditAnEmptyBalance() {
        Balance balance = builder().availableAmount(money("0")).reservedAmount(money("0")).build();
        balance.credit(money("0.01"));
        assertAmounts(balance, "0.01", "0");
    }

    @ParameterizedTest
    @EnumSource(BalanceMovementType.class)
    void rejectsNullAndZeroForEveryOperationWithoutMutation(BalanceMovementType type) {
        Balance balance = builder().build();
        assertError(BalanceErrorCode.INVALID_AMOUNT, () -> apply(balance, type, null));
        assertError(BalanceErrorCode.INVALID_AMOUNT, () -> apply(balance, type, money("0")));
        assertUnchanged(balance);
    }

    @ParameterizedTest
    @EnumSource(BalanceMovementType.class)
    void rejectsCurrencyMismatchForEveryOperationWithoutMutation(BalanceMovementType type) {
        Balance balance = builder().build();
        Money eur = Money.of(BigDecimal.ONE, CurrencyCode.of("EUR"));
        assertError(BalanceErrorCode.CURRENCY_MISMATCH, () -> apply(balance, type, eur));
        assertUnchanged(balance);
    }

    @ParameterizedTest
    @EnumSource(value = BalanceMovementType.class, names = {"RESERVE", "RELEASE", "DEBIT_RESERVED"})
    void rejectsInsufficientFundsWithoutMutation(BalanceMovementType type) {
        Balance balance = builder().build();
        BalanceErrorCode error = type == BalanceMovementType.RESERVE
                ? BalanceErrorCode.INSUFFICIENT_AVAILABLE_BALANCE : BalanceErrorCode.INSUFFICIENT_RESERVED_BALANCE;
        assertError(error, () -> apply(balance, type, money("101")));
        assertUnchanged(balance);
    }

    @Test
    void builderRejectsMissingParametersAndInvalidState() {
        List<Consumer<Balance.BalanceBuilder>> invalidParams = List.of(
                b -> b.balanceId(null), b -> b.balanceAccountId(null), b -> b.currency(null),
                b -> b.createdAt(null), b -> b.updatedAt(null));
        for (Consumer<Balance.BalanceBuilder> mutation : invalidParams) {
            Balance.BalanceBuilder builder = builder();
            mutation.accept(builder);
            assertError(BalanceErrorCode.INVALID_PARAMS, builder::build);
        }
        List<Consumer<Balance.BalanceBuilder>> invalidState = List.of(
                b -> b.availableAmount(null), b -> b.reservedAmount(null),
                b -> b.version(null), b -> b.version(-1L));
        for (Consumer<Balance.BalanceBuilder> mutation : invalidState) {
            Balance.BalanceBuilder builder = builder();
            mutation.accept(builder);
            assertError(BalanceErrorCode.INVALID_BALANCE_STATE, builder::build);
        }
    }

    @Test
    void builderRequiresBothAmountsToMatchBalanceCurrency() {
        Money eur = Money.of(BigDecimal.ONE, CurrencyCode.of("EUR"));
        assertError(BalanceErrorCode.CURRENCY_MISMATCH, () -> builder().availableAmount(eur).build());
        assertError(BalanceErrorCode.CURRENCY_MISMATCH, () -> builder().reservedAmount(eur).build());
    }

    private static Balance.BalanceBuilder builder() {
        return Balance.builder().balanceId(BalanceId.of(1L)).balanceAccountId(BalanceAccountId.of(2L))
                .currency(USD).availableAmount(money("100")).reservedAmount(money("20"))
                .version(3L).createdAt(CREATED).updatedAt(CREATED);
    }

    private static Money money(String value) {
        return Money.ofNonNegative(new BigDecimal(value), USD);
    }

    private static void apply(Balance balance, BalanceMovementType type, Money amount) {
        switch (type) {
            case CREDIT -> balance.credit(amount);
            case RESERVE -> balance.reserve(amount);
            case RELEASE -> balance.release(amount);
            case DEBIT_RESERVED -> balance.debitReserved(amount);
        }
    }

    private static void assertAmounts(Balance balance, String available, String reserved) {
        assertEquals(money(available), balance.getAvailableAmount());
        assertEquals(money(reserved), balance.getReservedAmount());
    }

    private static void assertUnchanged(Balance balance) {
        assertAmounts(balance, "100", "20");
        assertEquals(CREATED, balance.getUpdatedAt());
        assertEquals(3L, balance.getVersion());
    }

    private static void assertError(BalanceErrorCode error, Runnable action) {
        assertEquals(error, assertThrows(BalanceDomainException.class, action::run).errorCode());
    }
}
