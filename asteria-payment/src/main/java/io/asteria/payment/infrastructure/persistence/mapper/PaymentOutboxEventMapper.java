package io.asteria.payment.infrastructure.persistence.mapper;

import io.asteria.payment.infrastructure.persistence.dataobject.PaymentOutboxEventDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.time.Instant;
import java.util.List;

/**
 * @author 61421
 * @description 针对表【payment_outbox_event(支付模块本地消息表，用于 Transactional Outbox，保证支付状态变更与待发送事件在同一本地事务中持久化)】的数据库操作Mapper
 * @createDate 2026-08-30 21:34:34
 * @Entity io.asteria.payment.infrastructure.persistence.dataobject.PaymentOutboxEventDO
 */
@Mapper
public interface PaymentOutboxEventMapper extends BaseMapper<PaymentOutboxEventDO> {

    /**
     * 仅按事件事实的业务时间筛选，SQL 位于同名 XML。
     */
    List<PaymentOutboxEventDO> findCapturedInWindow(@Param("start") Instant start, @Param("end") Instant end,
            @Param("afterId") long afterId, @Param("limit") int limit);

}




