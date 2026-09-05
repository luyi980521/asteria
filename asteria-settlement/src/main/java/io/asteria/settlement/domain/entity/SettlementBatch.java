package io.asteria.settlement.domain.entity;

import io.asteria.common.domain.valueobject.Money;
import io.asteria.settlement.domain.enums.SettlementBatchStatus;
import io.asteria.settlement.domain.error.SettlementErrorCode;
import io.asteria.settlement.domain.exception.SettlementDomainException;
import io.asteria.settlement.domain.valueobject.SettlementBatchId;
import io.asteria.settlement.domain.valueobject.SettlementBatchReference;
import lombok.Builder;
import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;

import java.time.Instant;
import java.util.List;

/**
 * 结算批次。
 */
@Getter
@Builder
public class SettlementBatch {

    /**
     * 结算批次 ID
     */
    private final SettlementBatchId settlementBatchId;

    /**
     * 结算批次引用
     */
    private final SettlementBatchReference reference;

    /**
     * 币种
     */
    private final String currency;

    /**
     * 总金额
     */
    private final Money grossAmount;

    /**
     * 手续费
     */
    private final Money feeAmount;

    /**
     * 净结算金额
     */
    private final Money netAmount;

    /**
     * 结算明细
     */
    private final List<SettlementItem> items;

    /**
     * 状态
     */
    private SettlementBatchStatus status;

    /**
     * 上游渠道结算批次 ID
     */
    private String channelSettlementBatchId;

    /**
     * 创建时间
     */
    private final Instant createdAt;

    /**
     * 开始处理时间
     */
    private Instant processingAt;

    /**
     * 渠道受理时间
     */
    private Instant acceptedAt;

    /**
     * 实际结算完成时间
     */
    private Instant settledAt;

    /**
     * 失败时间
     */
    private Instant failedAt;

    /**
     * 创建结算批次。
     */
    public static SettlementBatch create(SettlementBatchId settlementBatchId,
                                         SettlementBatchReference reference,
                                         Money grossAmount,
                                         Money feeAmount,
                                         Money netAmount,
                                         List<SettlementItem> items,
                                         Instant createdAt) {
        validateItems(items);
        validateAmounts(grossAmount, feeAmount, netAmount, items);

        return SettlementBatch.builder()
                .settlementBatchId(settlementBatchId)
                .reference(reference)
                .currency(grossAmount.currency().getSymbol())
                .grossAmount(grossAmount)
                .feeAmount(feeAmount)
                .netAmount(netAmount)
                .items(List.copyOf(items))
                .status(SettlementBatchStatus.CREATED)
                .createdAt(createdAt)
                .build();
    }

    /**
     * 开始处理结算批次。
     */
    public void startProcessing(Instant processingAt) {
        if (status != SettlementBatchStatus.CREATED) {
            throw new SettlementDomainException(SettlementErrorCode.INVALID_SETTLEMENT_BATCH_STATUS);
        }

        this.status = SettlementBatchStatus.PROCESSING;
        this.processingAt = processingAt;
    }

    /**
     * 标记渠道已受理。
     */
    public void accept(String channelSettlementBatchId, Instant acceptedAt) {
        if (status != SettlementBatchStatus.PROCESSING) {
            throw new SettlementDomainException(SettlementErrorCode.INVALID_SETTLEMENT_BATCH_STATUS);
        }

        this.status = SettlementBatchStatus.ACCEPTED;
        this.channelSettlementBatchId = channelSettlementBatchId;
        this.acceptedAt = acceptedAt;
    }

    /**
     * 标记提交结果未知。
     */
    public void markSubmitUnknown() {
        if (status != SettlementBatchStatus.PROCESSING) {
            throw new SettlementDomainException(SettlementErrorCode.INVALID_SETTLEMENT_BATCH_STATUS);
        }

        this.status = SettlementBatchStatus.SUBMIT_UNKNOWN;
    }

    /**
     * 标记结算完成。
     */
    public void settle(Instant settledAt) {
        if (status != SettlementBatchStatus.ACCEPTED) {
            throw new SettlementDomainException(SettlementErrorCode.INVALID_SETTLEMENT_BATCH_STATUS);
        }

        this.status = SettlementBatchStatus.SETTLED;
        this.settledAt = settledAt;
    }

    /**
     * 标记结算失败。
     */
    public void fail(Instant failedAt) {
        if (status != SettlementBatchStatus.PROCESSING
                && status != SettlementBatchStatus.ACCEPTED) {
            throw new SettlementDomainException(SettlementErrorCode.INVALID_SETTLEMENT_BATCH_STATUS);
        }

        this.status = SettlementBatchStatus.FAILED;
        this.failedAt = failedAt;
    }

    /**
     * 重建结算批次。
     */
    public static SettlementBatch reconstitute(SettlementBatchId settlementBatchId,
                                               SettlementBatchReference reference,
                                               String currency,
                                               Money grossAmount,
                                               Money feeAmount,
                                               Money netAmount,
                                               List<SettlementItem> items,
                                               SettlementBatchStatus status,
                                               String channelSettlementBatchId,
                                               Instant createdAt,
                                               Instant processingAt,
                                               Instant acceptedAt,
                                               Instant settledAt,
                                               Instant failedAt) {
        return SettlementBatch.builder()
                .settlementBatchId(settlementBatchId)
                .reference(reference)
                .currency(currency)
                .grossAmount(grossAmount)
                .feeAmount(feeAmount)
                .netAmount(netAmount)
                .items(List.copyOf(items))
                .status(status)
                .channelSettlementBatchId(channelSettlementBatchId)
                .createdAt(createdAt)
                .processingAt(processingAt)
                .acceptedAt(acceptedAt)
                .settledAt(settledAt)
                .failedAt(failedAt)
                .build();
    }

    private static void validateItems(List<SettlementItem> items) {
        if (CollectionUtils.isEmpty(items)) {
            throw new SettlementDomainException(SettlementErrorCode.SETTLEMENT_ITEMS_REQUIRED);
        }
    }

    private static void validateAmounts(Money grossAmount,
                                        Money feeAmount,
                                        Money netAmount,
                                        List<SettlementItem> items) {
        // 下一步根据你现有 Money API 补齐
    }

    private void validateStatus(SettlementBatchStatus expectedStatus) {
        if (status != expectedStatus) {
            throw new SettlementDomainException(SettlementErrorCode.INVALID_SETTLEMENT_BATCH_STATUS);
        }
    }
}