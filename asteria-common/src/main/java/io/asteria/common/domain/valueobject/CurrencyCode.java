package io.asteria.common.domain.valueobject;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.asteria.common.domain.error.CommonErrorCode;
import io.asteria.common.domain.exception.CommonDomainException;
import org.apache.commons.lang3.StringUtils;

import java.util.Locale;

/** A syntactically valid currency code, independent of currency master data. */
public record CurrencyCode(String value) {

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public CurrencyCode {
        if (StringUtils.isBlank(value)) {
            throw new CommonDomainException(CommonErrorCode.CURRENCY_CODE_REQUIRED);
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!normalized.matches("^[A-Z]{3}$")) {
            throw new CommonDomainException(CommonErrorCode.INVALID_CURRENCY_CODE);
        }
        value = normalized;
    }

    public static CurrencyCode of(String value) {
        return new CurrencyCode(value);
    }

    @JsonValue
    public String value() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}
