package io.asteria.reconciliation.domain.entity;

import io.asteria.common.domain.valueobject.Money;
import io.asteria.reconciliation.domain.enums.ReconciliationSourceType;
import io.asteria.reconciliation.domain.valueobject.ReconciliationSourceRecordId;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

import static io.asteria.reconciliation.domain.entity.ReconciliationValidation.*;

/**
 * 来源业务的不可变标准化快照；同一来源记录不同版本可并存，不依附单个 Run。
 */
@Getter
public final class ReconciliationSourceRecord {
    /** 主键，由 DistributedIdGenerator 生成正数雪花 ID。 */
    private final ReconciliationSourceRecordId recordId;

    /** 来源系统类型，与原始标识和版本共同构成幂等键。 */
    private final ReconciliationSourceType sourceType;

    /** 来源系统原始记录 ID，跨渠道时需带命名空间。 */
    private final String sourceRecordId;

    /** 来源记录的单调非负版本；无原生版本时采集方须提供稳定映射。 */
    private final Long sourceVersion;

    /** 规则约定的跨来源业务关联键；须包含必要的租户或渠道命名空间。 */
    private final String businessKey;

    /** 标准化金额，允许零；未知金额整体为空。 */
    private final Money amount;

    /** 原始业务状态，保留来源语义，由规则解释。 */
    private final String sourceStatus;

    /** 可选的入账或记账状态，由规则解释。 */
    private final String postingStatus;

    /** 来源业务发生时间，按此字段落入左闭右开对账窗口。 */
    private final Instant occurredAt;

    /** JsonUtils 序列化的原始业务快照 JSON 对象；不可修改。 */
    private final String payload;

    /** 记录创建时间，UTC 时间点。 */
    private final Instant createdAt;

    /**
     * 构造或恢复领域对象；Builder 同样执行校验，不能绕过数据及状态约束。
     */
    @Builder
    private ReconciliationSourceRecord(
            ReconciliationSourceRecordId recordId,
            ReconciliationSourceType sourceType,
            String sourceRecordId,
            Long sourceVersion,
            String businessKey,
            Money amount,
            String sourceStatus,
            String postingStatus,
            Instant occurredAt,
            String payload,
            Instant createdAt) {
        require(recordId != null);
        require(sourceType != null);
        text(sourceRecordId, 128);
        require(sourceVersion != null);
        text(businessKey, 256);
        money(amount);
        text(sourceStatus, 64);
        if (postingStatus != null) { text(postingStatus, 64); }
        require(occurredAt != null);
        json(payload, true);
        require(createdAt != null);
        require(sourceVersion >= 0 && !occurredAt.isAfter(createdAt));
        this.recordId = recordId;
        this.sourceType = sourceType;
        this.sourceRecordId = sourceRecordId;
        this.sourceVersion = sourceVersion;
        this.businessKey = businessKey;
        this.amount = amount;
        this.sourceStatus = sourceStatus;
        this.postingStatus = postingStatus;
        this.occurredAt = occurredAt;
        this.payload = payload;
        this.createdAt = createdAt;
    }
}
