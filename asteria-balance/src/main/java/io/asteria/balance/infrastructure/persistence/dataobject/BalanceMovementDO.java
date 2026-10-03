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
@TableName("balance_movement")
public class BalanceMovementDO {
    @TableId(type = IdType.INPUT)
    private Long id;
    private Long balanceId;
    private String movementType;
    private BigDecimal amount;
    private String currency;
    private String referenceType;
    private String referenceId;
    private String eventId;
    private Instant createdAt;
}
