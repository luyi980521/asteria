package io.asteria.settlement.infrastructure.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 结算批次表，记录一组待结算资金的批次级状态与金额汇总
 * @TableName settlement_batch
 */
@TableName(value ="settlement_batch")
@Data
@Builder
public class SettlementBatchDO {
    /**
     * 结算批次 ID，使用雪花 ID
     */
    @TableId
    private Long id;

    /**
     * 结算批次业务唯一引用
     */
    private String settlementReference;

    /**
     * 结算币种
     */
    private String currency;

    /**
     * 批次结算总金额，未扣除手续费
     */
    private BigDecimal grossAmount;

    /**
     * 批次手续费金额
     */
    private BigDecimal feeAmount;

    /**
     * 批次净结算金额，通常等于 gross_amount - fee_amount
     */
    private BigDecimal netAmount;

    /**
     * 结算批次状态：CREATED-已创建，PROCESSING-处理中，ACCEPTED-渠道已受理，SETTLED-已结算，FAILED-失败
     */
    private String status;

    /**
     * 上游渠道返回的结算批次 ID
     */
    private String channelSettlementBatchId;

    /**
     * 结算批次创建时间
     */
    private Instant createdAt;

    /**
     * 开始处理结算批次的时间
     */
    private Instant processingAt;

    /**
     * 上游渠道受理结算批次的时间
     */
    private Instant acceptedAt;

    /**
     * 结算资金实际完成的时间
     */
    private Instant settledAt;

    /**
     * 结算批次失败时间
     */
    private Instant failedAt;

    /**
     * 最后更新时间
     */
    private Instant updatedAt;
}