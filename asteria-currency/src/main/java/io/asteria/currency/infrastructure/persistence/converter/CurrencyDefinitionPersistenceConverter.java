package io.asteria.currency.infrastructure.persistence.converter;

import io.asteria.common.domain.valueobject.CurrencyCode;
import io.asteria.currency.domain.model.CurrencyDefinition;
import io.asteria.currency.infrastructure.persistence.dataobject.CurrencyDefinitionDO;
import org.springframework.stereotype.Component;

@Component
public class CurrencyDefinitionPersistenceConverter {
    public CurrencyDefinitionDO toDataObject(CurrencyDefinition definition) {
        return CurrencyDefinitionDO.builder()
                .code(definition.getCode().value())
                .numericCode(definition.getNumericCode())
                .displayName(definition.getDisplayName())
                .symbol(definition.getSymbol())
                .minorUnit(definition.getMinorUnit())
                .enabled(definition.isEnabled())
                .createdAt(definition.getCreatedAt())
                .updatedAt(definition.getUpdatedAt())
                .build();
    }

    public CurrencyDefinition toDomain(CurrencyDefinitionDO dataObject) {
        return CurrencyDefinition.builder()
                .code(CurrencyCode.of(dataObject.getCode()))
                .numericCode(dataObject.getNumericCode())
                .displayName(dataObject.getDisplayName())
                .symbol(dataObject.getSymbol())
                .minorUnit(dataObject.getMinorUnit())
                .enabled(dataObject.isEnabled())
                .createdAt(dataObject.getCreatedAt())
                .updatedAt(dataObject.getUpdatedAt())
                .build();
    }
}
