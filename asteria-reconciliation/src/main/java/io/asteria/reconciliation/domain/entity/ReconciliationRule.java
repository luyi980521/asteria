package io.asteria.reconciliation.domain.entity;

import io.asteria.reconciliation.domain.enums.ReconciliationSourceType;
import io.asteria.reconciliation.domain.enums.ReconciliationType;
import io.asteria.reconciliation.domain.valueobject.ReconciliationRuleId;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

import static io.asteria.reconciliation.domain.entity.ReconciliationValidation.*;

/**
 * 对账规则定义；编码稳定，配置变更递增版本，Run 保存版本与快照以便审计。
 */
@Getter
public final class ReconciliationRule {
    /** 主键，由 DistributedIdGenerator 生成正数雪花 ID。 */
    private final ReconciliationRuleId ruleId;

    /** 稳定规则编码，逻辑关联 reconciliation_rule.rule_code。 */
    private final String ruleCode;

    /** 业务可读的规则名称。 */
    private final String name;

    /** 对账关系类型。 */
    private final ReconciliationType type;

    /** 基准来源；v1 为 PAYMENT 或 SETTLEMENT。 */
    private final ReconciliationSourceType leftSourceType;

    /** 被核对来源；v1 必须为 LEDGER。 */
    private final ReconciliationSourceType rightSourceType;

    /** 是否允许创建新的对账运行。 */
    private boolean enabled;

    /** 等待异步入账的宽限秒数，非负；窗口结束加宽限后才可判定缺失。 */
    private Long gracePeriodSeconds;

    /** 使用 JsonUtils 序列化的 JSON 说明对象，以 TEXT 原样保存。 */
    private String details;

    /** 乐观锁版本；更新使用旧版本匹配并由数据库递增，更新后应重新读取。 */
    private final Long version;

    /** 记录创建时间，UTC 时间点。 */
    private final Instant createdAt;

    /** 最后更新时间，UTC 时间点且不得早于创建时间。 */
    private Instant updatedAt;

    /**
     * 构造或恢复领域对象；Builder 同样执行校验，不能绕过数据及状态约束。
     */
    @Builder
    private ReconciliationRule(
            ReconciliationRuleId ruleId,
            String ruleCode,
            String name,
            ReconciliationType type,
            ReconciliationSourceType leftSourceType,
            ReconciliationSourceType rightSourceType,
            boolean enabled,
            Long gracePeriodSeconds,
            String details,
            Long version,
            Instant createdAt,
            Instant updatedAt) {
        require(ruleId != null);
        text(ruleCode, 64);
        text(name, 128);
        require(type != null);
        require(leftSourceType != null);
        require(rightSourceType != null);
        require(gracePeriodSeconds != null);
        json(details, false);
        require(version != null);
        require(createdAt != null);
        require(updatedAt != null);
        require(version >= 0);
        times(createdAt, updatedAt);
        require(rightSourceType == ReconciliationSourceType.LEDGER
                && ((type == ReconciliationType.PAYMENT_LEDGER && leftSourceType == ReconciliationSourceType.PAYMENT)
                || (type == ReconciliationType.SETTLEMENT_LEDGER && leftSourceType == ReconciliationSourceType.SETTLEMENT)));
        require(gracePeriodSeconds >= 0);
        this.ruleId = ruleId;
        this.ruleCode = ruleCode;
        this.name = name;
        this.type = type;
        this.leftSourceType = leftSourceType;
        this.rightSourceType = rightSourceType;
        this.enabled = enabled;
        this.gracePeriodSeconds = gracePeriodSeconds;
        this.details = details;
        this.version = version;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * 创建启用的规则，版本从零开始；只支持 v1 的两种来源组合。
     */
    public static ReconciliationRule create(ReconciliationRuleId ruleId, String ruleCode, String name,
                                             ReconciliationType type, Long gracePeriodSeconds, String details, Instant now) {
        return builder().ruleId(ruleId).ruleCode(ruleCode).name(name).type(type)
                .leftSourceType(type == ReconciliationType.PAYMENT_LEDGER
                        ? ReconciliationSourceType.PAYMENT : ReconciliationSourceType.SETTLEMENT)
                .rightSourceType(ReconciliationSourceType.LEDGER).enabled(true)
                .gracePeriodSeconds(gracePeriodSeconds).details(details).version(0L).createdAt(now).updatedAt(now).build();
    }

    /**
     * 调整启用状态及等待期，规则身份和来源组合不可修改；保存时递增版本。
     */
    public void configure(boolean enabled, Long gracePeriodSeconds, String details, Instant now) {
        require(gracePeriodSeconds != null && gracePeriodSeconds >= 0);
        json(details, false);
        nextTime(updatedAt, now);
        this.enabled = enabled;
        this.gracePeriodSeconds = gracePeriodSeconds;
        this.details = details;
        this.updatedAt = now;
    }
}
