package io.asteria.currency.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.asteria.currency.infrastructure.persistence.dataobject.CurrencyDefinitionDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CurrencyDefinitionMapper extends BaseMapper<CurrencyDefinitionDO> {
}
