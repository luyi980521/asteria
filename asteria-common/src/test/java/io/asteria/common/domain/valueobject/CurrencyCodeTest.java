package io.asteria.common.domain.valueobject;

import io.asteria.common.domain.error.CommonErrorCode;
import io.asteria.common.domain.exception.CommonDomainException;
import io.asteria.common.util.JsonUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

class CurrencyCodeTest {
    @ParameterizedTest
    @ValueSource(strings = {"USD", "usd", " usd "})
    void normalizesCode(String input) {
        assertEquals("USD", CurrencyCode.of(input).value());
        assertEquals("USD", CurrencyCode.of(input).toString());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t\n"})
    void rejectsMissingCode(String input) {
        assertEquals(CommonErrorCode.CURRENCY_CODE_REQUIRED,
                assertThrows(CommonDomainException.class, () -> CurrencyCode.of(input)).errorCode());
    }

    @ParameterizedTest
    @ValueSource(strings = {"US", "USDD", "U1D", "123", "U D"})
    void rejectsInvalidCode(String input) {
        assertEquals(CommonErrorCode.INVALID_CURRENCY_CODE,
                assertThrows(CommonDomainException.class, () -> CurrencyCode.of(input)).errorCode());
    }

    @Test
    void normalizationDoesNotDependOnDefaultLocale() {
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            assertEquals("INR", CurrencyCode.of("inr").value());
        } finally {
            Locale.setDefault(original);
        }
    }

    @Test
    void currencyJsonIsAStringAndRoundTrips() {
        CurrencyCode currency = CurrencyCode.of("USD");
        assertEquals("\"USD\"", JsonUtils.toJson(currency));
        assertEquals(currency, JsonUtils.fromJson("\"USD\"", CurrencyCode.class));
        assertEquals(currency, JsonUtils.fromJson("\" usd \"", CurrencyCode.class));
    }

    @Test
    void moneyJsonRetainsAmountAndStringCurrency() {
        Money money = Money.of(new BigDecimal("100"), CurrencyCode.of("USD"));
        String json = JsonUtils.toJson(money);
        assertEquals(2, JsonUtils.readTree(json).size());
        assertEquals("USD", JsonUtils.readTree(json).get("currency").asText());
        assertEquals(money, JsonUtils.fromJson(json, Money.class));
        assertEquals(money, JsonUtils.fromJson("{\"amount\":100,\"currency\":\"USD\"}", Money.class));
        assertThrows(IllegalArgumentException.class,
                () -> JsonUtils.fromJson("{\"amount\":0,\"currency\":\"USD\"}", Money.class));
    }

    @Test
    void rejectsCrossCurrencyArithmetic() {
        Money usd = Money.of(new BigDecimal("100"), CurrencyCode.of("USD"));
        Money eur = Money.of(new BigDecimal("100"), CurrencyCode.of("EUR"));
        assertEquals(CommonErrorCode.MONEY_CURRENCY_MISMATCH,
                assertThrows(CommonDomainException.class, () -> usd.add(eur)).errorCode());
    }

    @Test
    void retainsPrecisionRulesAndScaleIndependentEquality() {
        assertThrows(CommonDomainException.class,
                () -> Money.of(new BigDecimal("1.1"), CurrencyCode.of("JPY")));
        assertThrows(CommonDomainException.class,
                () -> Money.of(new BigDecimal("1.001"), CurrencyCode.of("USD")));
        assertEquals(new BigDecimal("1.123"),
                Money.of(new BigDecimal("1.123"), CurrencyCode.of("KWD")).amount());
        assertEquals(new BigDecimal("1.1234"),
                Money.of(new BigDecimal("1.1234"), CurrencyCode.of("CLF")).amount());
        Money first = Money.of(new BigDecimal("1.0"), CurrencyCode.of("USD"));
        Money second = Money.of(new BigDecimal("1.00"), CurrencyCode.of("USD"));
        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
        assertEquals("ZZZ", CurrencyCode.of("ZZZ").value());
        assertThrows(CommonDomainException.class, () -> Money.of(BigDecimal.ONE, CurrencyCode.of("ZZZ")));
    }
}
