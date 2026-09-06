package io.asteria.common.domain.valueobject;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/** Preserves Money's existing precision rules without runtime metadata or database lookups. */
final class MoneyCurrencyScale {
    private static final Properties SCALES = loadScales();

    private MoneyCurrencyScale() {
    }

    static int fractionDigits(CurrencyCode code) {
        return Integer.parseInt(SCALES.getProperty(code.value(), "-1"));
    }

    private static Properties loadScales() {
        try (InputStream input = MoneyCurrencyScale.class.getResourceAsStream(
                "/io/asteria/common/money-currency-scales.properties")) {
            if (input == null) {
                throw new IllegalStateException("Money currency scale snapshot is missing");
            }
            Properties scales = new Properties();
            scales.load(input);
            return scales;
        } catch (IOException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }
}
