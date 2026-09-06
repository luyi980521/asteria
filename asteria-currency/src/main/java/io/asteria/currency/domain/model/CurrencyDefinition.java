package io.asteria.currency.domain.model;

import io.asteria.common.domain.valueobject.CurrencyCode;
import io.asteria.currency.domain.error.CurrencyErrorCode;
import io.asteria.currency.domain.exception.CurrencyDomainException;
import lombok.Builder;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.time.Instant;

@Getter
public final class CurrencyDefinition {
    private final CurrencyCode code;
    private final String numericCode;
    private final String displayName;
    private final String symbol;
    private final int minorUnit;
    private final boolean enabled;
    private final Instant createdAt;
    private final Instant updatedAt;

    @Builder
    private CurrencyDefinition(CurrencyCode code, String numericCode, String displayName,
                               String symbol, int minorUnit, boolean enabled,
                               Instant createdAt, Instant updatedAt) {
        if (code == null) {
            throw new CurrencyDomainException(CurrencyErrorCode.CURRENCY_CODE_REQUIRED);
        }
        if (StringUtils.isBlank(displayName)) {
            throw new CurrencyDomainException(CurrencyErrorCode.DISPLAY_NAME_REQUIRED);
        }
        if (minorUnit < 0 || minorUnit > 4) {
            throw new CurrencyDomainException(CurrencyErrorCode.INVALID_MINOR_UNIT);
        }
        String normalizedNumericCode = StringUtils.trimToNull(numericCode);
        if (normalizedNumericCode != null && !normalizedNumericCode.matches("^[0-9]{3}$")) {
            throw new CurrencyDomainException(CurrencyErrorCode.INVALID_NUMERIC_CODE);
        }
        if (createdAt == null) {
            throw new CurrencyDomainException(CurrencyErrorCode.CREATED_AT_REQUIRED);
        }
        if (updatedAt == null) {
            throw new CurrencyDomainException(CurrencyErrorCode.UPDATED_AT_REQUIRED);
        }
        this.code = code;
        this.numericCode = normalizedNumericCode;
        this.displayName = displayName;
        this.symbol = symbol;
        this.minorUnit = minorUnit;
        this.enabled = enabled;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
