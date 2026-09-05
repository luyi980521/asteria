package io.asteria.settlement.infrastructure.persistence.mapper;

import io.asteria.settlement.infrastructure.persistence.dataobject.SettlementOutboxEventDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
* @author 61421
* @description 针对表【settlement_outbox_event(结算模块本地消息表，用于 Transactional Outbox，保证结算状态与结算事件原子持久化)】的数据库操作Mapper
* @createDate 2026-09-06 00:59:58
* @Entity io.asteria.settlement.infrastructure.persistence.dataobject.SettlementOutboxEventDO
*/
@Mapper
public interface SettlementOutboxEventMapper extends BaseMapper<SettlementOutboxEventDO> {

}




