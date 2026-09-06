package io.asteria.currency.domain.repository;

import io.asteria.common.domain.valueobject.CurrencyCode;
import io.asteria.currency.domain.model.CurrencyDefinition;

import java.util.List;

public interface CurrencyDefinitionRepository {
    void insert(CurrencyDefinition currencyDefinition);

    void update(CurrencyDefinition currencyDefinition);

    /** Returns null when the code does not exist. */
    CurrencyDefinition findByCode(CurrencyCode code);

    List<CurrencyDefinition> findAllEnabled();
}
