package io.asteria.reconciliation.infrastructure.persistence.converter;

import io.asteria.reconciliation.domain.entity.ReconciliationCaseAction;
import io.asteria.reconciliation.domain.enums.ReconciliationCaseActionType;
import io.asteria.reconciliation.domain.enums.ReconciliationCaseStatus;
import io.asteria.reconciliation.domain.enums.ReconciliationOperatorType;
import io.asteria.reconciliation.domain.valueobject.ReconciliationCaseActionId;
import io.asteria.reconciliation.domain.valueobject.ReconciliationCaseId;
import io.asteria.reconciliation.infrastructure.persistence.dataobject.ReconciliationCaseActionDO;
import org.springframework.stereotype.Component;

/**
 * ReconciliationCaseAction 与数据库行的显式转换；保留 String/TEXT JSON，不重复序列化。
 */
@Component
public class ReconciliationCaseActionPersistenceConverter {
    /**
     * 将类型化 ID、枚举及 Money 拆分为现有数据库列类型。
     */
    public ReconciliationCaseActionDO toDataObject(ReconciliationCaseAction domain) {
        return ReconciliationCaseActionDO.builder()
                .id(domain.getCaseActionId().value())
                .caseId(domain.getCaseId().value())
                .actionType(domain.getActionType().name())
                .operatorType(domain.getOperatorType().name())
                .operatorId(domain.getOperatorId())
                .fromStatus(domain.getFromStatus() == null ? null : domain.getFromStatus().name())
                .toStatus(domain.getToStatus().name())
                .reason(domain.getReason())
                .details(domain.getDetails())
                .createdAt(domain.getCreatedAt())
                .build();
    }

    /**
     * 从持久化数据恢复，重新执行 Builder 的基本领域约束。
     */
    public ReconciliationCaseAction toDomain(ReconciliationCaseActionDO dataObject) {
        return ReconciliationCaseAction.builder()
                .caseActionId(ReconciliationCaseActionId.of(dataObject.getId()))
                .caseId(ReconciliationCaseId.of(dataObject.getCaseId()))
                .actionType(ReconciliationCaseActionType.valueOf(dataObject.getActionType()))
                .operatorType(ReconciliationOperatorType.valueOf(dataObject.getOperatorType()))
                .operatorId(dataObject.getOperatorId())
                .fromStatus(dataObject.getFromStatus() == null ? null : ReconciliationCaseStatus.valueOf(dataObject.getFromStatus()))
                .toStatus(ReconciliationCaseStatus.valueOf(dataObject.getToStatus()))
                .reason(dataObject.getReason())
                .details(dataObject.getDetails())
                .createdAt(dataObject.getCreatedAt())
                .build();
    }
}
