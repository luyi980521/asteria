package io.asteria.settlement.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.asteria.settlement.infrastructure.persistence.dataobject.SettlementItemDO;

import java.util.List;

/**
* @author 61421
* @description 针对表【settlement_item(结算批次明细表，记录结算批次包含的具体支付资金明细)】的数据库操作Mapper
* @createDate 2026-08-31 22:12:27
* @Entity io.asteria.settlement.infrastructure.persistence.dataobject.SettlementItemDO
*/
public interface SettlementItemMapper extends BaseMapper<SettlementItemDO> {

    /**
     * 批量插入结算明细。
     */
    int batchInsert(List<SettlementItemDO> items);
}




