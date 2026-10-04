package io.asteria.balance.infrastructure.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("balance_account")
public class BalanceAccountDO {
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;
    private String ownerType;
    private Long ownerId;
    private String status;
    private Instant createdAt;
    private Instant updatedAt;
}
