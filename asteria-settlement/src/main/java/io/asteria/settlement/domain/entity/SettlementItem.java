package io.asteria.settlement.domain.entity;

import io.asteria.common.domain.valueobject.Money;
import io.asteria.settlement.domain.valueobject.SettlementItemId;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

/**
 * 结算批次明细。
 */
@Getter
@Builder
public class SettlementItem {

    /** 明细 ID */
    private final SettlementItemId settlementItemId;

    /** Payment ID */
    private final Long paymentId;

    /** Payment Reference */
    private final String paymentReference;

    /** 结算金额 */
    private final Money amount;

    /** 创建时间 */
    private final Instant createdAt;
}