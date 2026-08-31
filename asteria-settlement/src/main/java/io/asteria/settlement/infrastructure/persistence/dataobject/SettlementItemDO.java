package io.asteria.settlement.infrastructure.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 结算批次明细表，记录结算批次包含的具体支付资金明细
 * @TableName settlement_item
 */
@TableName(value ="settlement_item")
@Data
@Builder
public class SettlementItemDO {
    /**
     * 结算明细 ID，使用雪花 ID
     */
    @TableId
    private Long id;

    /**
     * 所属结算批次 ID
     */
    private Long settlementBatchId;

    /**
     * 关联的 Payment ID
     */
    private Long paymentId;

    /**
     * 关联的 Payment 业务引用
     */
    private String paymentReference;

    /**
     * 该支付明细参与结算的金额
     */
    private BigDecimal amount;

    /**
     * 该支付明细的币种
     */
    private String currency;

    /**
     * 结算明细创建时间
     */
    private Instant createdAt;
}