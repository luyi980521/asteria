package io.asteria.settlement.infrastructure.persistence.mapper;

import io.asteria.settlement.infrastructure.persistence.dataobject.SettlementBatchDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
* @author 61421
* @description 针对表【settlement_batch(结算批次表，记录一组待结算资金的批次级状态与金额汇总)】的数据库操作Mapper
* @createDate 2026-08-31 22:12:27
* @Entity io.asteria.settlement.infrastructure.persistence.dataobject.SettlementBatchDO
*/
@Mapper
public interface SettlementBatchMapper extends BaseMapper<SettlementBatchDO> {

}




