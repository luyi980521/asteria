package io.asteria.balance.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.asteria.balance.infrastructure.persistence.dataobject.BalanceReservationDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface BalanceReservationMapper extends BaseMapper<BalanceReservationDO> {
    BalanceReservationDO findByReservationId(@Param("id") Long id);

    BalanceReservationDO findByBalanceIdAndReference(@Param("balanceId") Long balanceId,
                                                   @Param("referenceType") String referenceType,
                                                   @Param("referenceId") String referenceId);

    int updateStatus(BalanceReservationDO dataObject);
}
