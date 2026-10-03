package io.asteria.balance.infrastructure.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("balance")
public class BalanceDO {
    @TableId(type = IdType.INPUT)
    private Long id;
    private Long balanceAccountId;
    private String currency;
    private BigDecimal availableAmount;
    private BigDecimal reservedAmount;
    /** 数据库版本，条件更新由 Mapper XML 比较并递增。 */
    private Long version;
    private Instant createdAt;
    private Instant updatedAt;
}
