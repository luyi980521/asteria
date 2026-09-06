package io.asteria.currency.application.service;

import io.asteria.common.domain.valueobject.CurrencyCode;
import io.asteria.currency.domain.error.CurrencyErrorCode;
import io.asteria.currency.domain.exception.CurrencyDomainException;
import io.asteria.currency.domain.model.CurrencyDefinition;
import io.asteria.currency.domain.repository.CurrencyDefinitionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CurrencyDefinitionService {
    private final CurrencyDefinitionRepository repository;

    public CurrencyDefinition getByCode(CurrencyCode code) {
        if (code == null) {
            throw new CurrencyDomainException(CurrencyErrorCode.CURRENCY_CODE_REQUIRED);
        }
        CurrencyDefinition definition = repository.findByCode(code);
        if (definition == null) {
            log.debug("Currency definition not found: {}", code.value());
            throw new CurrencyDomainException(CurrencyErrorCode.CURRENCY_NOT_FOUND);
        }
        return definition;
    }

    public List<CurrencyDefinition> getEnabledCurrencies() {
        return repository.findAllEnabled();
    }
}
