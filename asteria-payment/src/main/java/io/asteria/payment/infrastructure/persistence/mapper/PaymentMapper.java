package io.asteria.payment.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.asteria.payment.infrastructure.persistence.dataobject.PaymentDO;
import org.apache.ibatis.annotations.Mapper;

/** Payment 数据库操作 Mapper */
@Mapper
public interface PaymentMapper extends BaseMapper<PaymentDO> {
}
