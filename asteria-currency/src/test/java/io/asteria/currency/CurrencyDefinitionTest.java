package io.asteria.currency;

import io.asteria.common.domain.valueobject.CurrencyCode;
import io.asteria.currency.application.service.CurrencyDefinitionService;
import io.asteria.currency.domain.error.CurrencyErrorCode;
import io.asteria.currency.domain.exception.CurrencyDomainException;
import io.asteria.currency.domain.model.CurrencyDefinition;
import io.asteria.currency.domain.repository.CurrencyDefinitionRepository;
import io.asteria.currency.infrastructure.persistence.converter.CurrencyDefinitionPersistenceConverter;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CurrencyDefinitionTest {
    private static final Instant NOW = Instant.parse("2026-09-06T00:00:00Z");

    private CurrencyDefinition.CurrencyDefinitionBuilder builder() {
        return CurrencyDefinition.builder().code(CurrencyCode.of("USD"))
                .numericCode("840").displayName("US Dollar").symbol("$")
                .minorUnit(2).enabled(true).createdAt(NOW).updatedAt(NOW);
    }

    @Test
    void validatesEveryConstructionPath() {
        assertError(CurrencyErrorCode.CURRENCY_CODE_REQUIRED, builder().code(null));
        assertError(CurrencyErrorCode.DISPLAY_NAME_REQUIRED, builder().displayName(" "));
        assertError(CurrencyErrorCode.INVALID_MINOR_UNIT, builder().minorUnit(-1));
        assertError(CurrencyErrorCode.INVALID_MINOR_UNIT, builder().minorUnit(5));
        for (String invalid : List.of("12", "1234", "A12", "１２３")) {
            assertError(CurrencyErrorCode.INVALID_NUMERIC_CODE, builder().numericCode(invalid));
        }
        assertError(CurrencyErrorCode.CREATED_AT_REQUIRED, builder().createdAt(null));
        assertError(CurrencyErrorCode.UPDATED_AT_REQUIRED, builder().updatedAt(null));
        assertNull(builder().numericCode(null).build().getNumericCode());
        assertNull(builder().numericCode(" ").build().getNumericCode());
        assertEquals(0, builder().minorUnit(0).build().getMinorUnit());
        assertEquals(4, builder().minorUnit(4).build().getMinorUnit());
    }

    @Test
    void persistenceRoundTripRetainsLeadingZerosAndTimestamps() {
        var converter = new CurrencyDefinitionPersistenceConverter();
        var definition = builder().code(CurrencyCode.of("ALL")).numericCode("008").enabled(false).build();
        var dataObject = converter.toDataObject(definition);
        assertEquals("ALL", dataObject.getCode());
        assertEquals("008", dataObject.getNumericCode());
        var restored = converter.toDomain(dataObject);
        assertEquals(definition.getCode(), restored.getCode());
        assertEquals("008", restored.getNumericCode());
        assertEquals(definition.getDisplayName(), restored.getDisplayName());
        assertEquals(definition.getSymbol(), restored.getSymbol());
        assertEquals(2, restored.getMinorUnit());
        assertFalse(restored.isEnabled());
        assertEquals(NOW, restored.getCreatedAt());
        assertEquals(NOW, restored.getUpdatedAt());
    }

    @Test
    void queryServiceReturnsDefinitionsAndOwnNotFoundError() {
        var repository = mock(CurrencyDefinitionRepository.class);
        var service = new CurrencyDefinitionService(repository);
        var definition = builder().build();
        when(repository.findByCode(definition.getCode())).thenReturn(definition);
        when(repository.findAllEnabled()).thenReturn(List.of(definition));
        assertSame(definition, service.getByCode(CurrencyCode.of("USD")));
        assertEquals(List.of(definition), service.getEnabledCurrencies());
        assertEquals(CurrencyErrorCode.CURRENCY_NOT_FOUND,
                assertThrows(CurrencyDomainException.class,
                        () -> service.getByCode(CurrencyCode.of("EUR"))).errorCode());
        assertEquals(CurrencyErrorCode.CURRENCY_CODE_REQUIRED,
                assertThrows(CurrencyDomainException.class, () -> service.getByCode(null)).errorCode());
    }

    private void assertError(CurrencyErrorCode expected, CurrencyDefinition.CurrencyDefinitionBuilder builder) {
        assertEquals(expected, assertThrows(CurrencyDomainException.class, builder::build).errorCode());
    }
}
