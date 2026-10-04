package io.asteria.balance.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.asteria.balance.infrastructure.persistence.dataobject.BalanceAccountDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface BalanceAccountMapper extends BaseMapper<BalanceAccountDO> {
    BalanceAccountDO findByBalanceAccountId(@Param("balanceAccountId") Long balanceAccountId);

    BalanceAccountDO findByOwner(@Param("ownerType") String ownerType, @Param("ownerId") Long ownerId);
}
