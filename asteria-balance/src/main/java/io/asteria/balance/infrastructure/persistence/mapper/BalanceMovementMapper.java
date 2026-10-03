package io.asteria.balance.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.asteria.balance.infrastructure.persistence.dataobject.BalanceMovementDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface BalanceMovementMapper extends BaseMapper<BalanceMovementDO> {
    boolean existsByEventIdAndBalanceIdAndMovementType(@Param("eventId") String eventId,
                                                     @Param("balanceId") Long balanceId,
                                                     @Param("movementType") String movementType);
}
