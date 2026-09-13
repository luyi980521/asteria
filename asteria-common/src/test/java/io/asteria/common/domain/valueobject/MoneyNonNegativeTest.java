package io.asteria.common.domain.valueobject;

import io.asteria.common.domain.error.CommonErrorCode;
import io.asteria.common.domain.exception.CommonDomainException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class MoneyNonNegativeTest {
    private static final CurrencyCode USD = CurrencyCode.of("USD");

    @Test
    void permitsZeroAndKeepsCurrencyScale() {
        assertEquals(new BigDecimal("0.00"), Money.ofNonNegative(BigDecimal.ZERO, USD).amount());
        assertEquals(Money.of(BigDecimal.ONE, USD), Money.ofNonNegative(BigDecimal.ONE, USD));
    }

    @Test
    void originalFactoryStillRequiresPositiveAmounts() {
        assertError(CommonErrorCode.MONEY_AMOUNT_MUST_BE_POSITIVE, () -> Money.of(BigDecimal.ZERO, USD));
        assertError(CommonErrorCode.MONEY_AMOUNT_MUST_BE_POSITIVE, () -> Money.of(BigDecimal.ONE.negate(), USD));
    }

    @Test
    void nonNegativeFactoryRejectsNegativeMissingAndInvalidScale() {
        assertError(CommonErrorCode.MONEY_AMOUNT_MUST_BE_NON_NEGATIVE,
                () -> Money.ofNonNegative(new BigDecimal("-0.01"), USD));
        assertError(CommonErrorCode.MONEY_AMOUNT_REQUIRED, () -> Money.ofNonNegative(null, USD));
        assertError(CommonErrorCode.MONEY_CURRENCY_REQUIRED, () -> Money.ofNonNegative(BigDecimal.ZERO, null));
        assertError(CommonErrorCode.MONEY_AMOUNT_SCALE_INVALID,
                () -> Money.ofNonNegative(new BigDecimal("0.001"), USD));
    }

    private static void assertError(CommonErrorCode error, Runnable action) {
        assertEquals(error, assertThrows(CommonDomainException.class, action::run).errorCode());
    }
}
