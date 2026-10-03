package io.asteria.balance.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.asteria.balance.infrastructure.persistence.dataobject.BalanceDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface BalanceMapper extends BaseMapper<BalanceDO> {
    BalanceDO findByBalanceId(@Param("id") Long id);

    BalanceDO findByBalanceAccountIdAndCurrency(@Param("balanceAccountId") Long balanceAccountId,
                                              @Param("currency") String currency);

    int updateWithVersion(BalanceDO dataObject);
}
