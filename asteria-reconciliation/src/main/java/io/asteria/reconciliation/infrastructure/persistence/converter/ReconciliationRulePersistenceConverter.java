package io.asteria.reconciliation.infrastructure.persistence.converter;

import io.asteria.reconciliation.domain.entity.ReconciliationRule;
import io.asteria.reconciliation.domain.enums.ReconciliationSourceType;
import io.asteria.reconciliation.domain.enums.ReconciliationType;
import io.asteria.reconciliation.domain.valueobject.ReconciliationRuleId;
import io.asteria.reconciliation.infrastructure.persistence.dataobject.ReconciliationRuleDO;
import org.springframework.stereotype.Component;

/**
 * ReconciliationRule 与数据库行的显式转换；保留 String/TEXT JSON，不重复序列化。
 */
@Component
public class ReconciliationRulePersistenceConverter {
    /**
     * 将类型化 ID、枚举及 Money 拆分为现有数据库列类型。
     */
    public ReconciliationRuleDO toDataObject(ReconciliationRule domain) {
        return ReconciliationRuleDO.builder()
                .id(domain.getRuleId().value())
                .ruleCode(domain.getRuleCode())
                .name(domain.getName())
                .type(domain.getType().name())
                .leftSourceType(domain.getLeftSourceType().name())
                .rightSourceType(domain.getRightSourceType().name())
                .enabled(domain.isEnabled())
                .gracePeriodSeconds(domain.getGracePeriodSeconds())
                .details(domain.getDetails())
                .version(domain.getVersion())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }

    /**
     * 从持久化数据恢复，重新执行 Builder 的基本领域约束。
     */
    public ReconciliationRule toDomain(ReconciliationRuleDO dataObject) {
        return ReconciliationRule.builder()
                .ruleId(ReconciliationRuleId.of(dataObject.getId()))
                .ruleCode(dataObject.getRuleCode())
                .name(dataObject.getName())
                .type(ReconciliationType.valueOf(dataObject.getType()))
                .leftSourceType(ReconciliationSourceType.valueOf(dataObject.getLeftSourceType()))
                .rightSourceType(ReconciliationSourceType.valueOf(dataObject.getRightSourceType()))
                .enabled(dataObject.isEnabled())
                .gracePeriodSeconds(dataObject.getGracePeriodSeconds())
                .details(dataObject.getDetails())
                .version(dataObject.getVersion())
                .createdAt(dataObject.getCreatedAt())
                .updatedAt(dataObject.getUpdatedAt())
                .build();
    }
}
