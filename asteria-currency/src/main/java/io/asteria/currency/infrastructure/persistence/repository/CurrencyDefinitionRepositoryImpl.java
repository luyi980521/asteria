package io.asteria.currency.infrastructure.persistence.repository;

import io.asteria.common.domain.valueobject.CurrencyCode;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.asteria.currency.domain.error.CurrencyErrorCode;
import io.asteria.currency.domain.exception.CurrencyDomainException;
import io.asteria.currency.domain.model.CurrencyDefinition;
import io.asteria.currency.domain.repository.CurrencyDefinitionRepository;
import io.asteria.currency.infrastructure.persistence.converter.CurrencyDefinitionPersistenceConverter;
import io.asteria.currency.infrastructure.persistence.dataobject.CurrencyDefinitionDO;
import io.asteria.currency.infrastructure.persistence.mapper.CurrencyDefinitionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;

@Slf4j
@Repository
@RequiredArgsConstructor
public class CurrencyDefinitionRepositoryImpl implements CurrencyDefinitionRepository {
    private final CurrencyDefinitionMapper mapper;
    private final CurrencyDefinitionPersistenceConverter converter;

    @Override
    public void insert(CurrencyDefinition definition) {
        log.info("Inserting currency definition: {}", definition.getCode().value());
        mapper.insert(converter.toDataObject(definition));
    }

    @Override
    public void update(CurrencyDefinition definition) {
        log.info("Updating currency definition: {}", definition.getCode().value());
        if (mapper.updateById(converter.toDataObject(definition)) == 0) {
            throw new CurrencyDomainException(CurrencyErrorCode.CURRENCY_NOT_FOUND);
        }
    }

    @Override
    public CurrencyDefinition findByCode(CurrencyCode code) {
        CurrencyDefinitionDO dataObject = mapper.selectById(code.value());
        return dataObject == null ? null : converter.toDomain(dataObject);
    }

    @Override
    public List<CurrencyDefinition> findAllEnabled() {
        return mapper.selectList(Wrappers.<CurrencyDefinitionDO>lambdaQuery()
                        .eq(CurrencyDefinitionDO::isEnabled, true)
                        .orderByAsc(CurrencyDefinitionDO::getCode))
                .stream().map(converter::toDomain).toList();
    }
}
