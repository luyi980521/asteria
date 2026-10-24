package io.asteria.reconciliation.infrastructure.persistence.converter;

import io.asteria.reconciliation.domain.entity.ReconciliationCase;
import io.asteria.reconciliation.domain.enums.ReconciliationCaseStatus;
import io.asteria.reconciliation.domain.enums.ReconciliationDifferenceType;
import io.asteria.reconciliation.domain.valueobject.ReconciliationCaseId;
import io.asteria.reconciliation.domain.valueobject.ReconciliationMatchResultId;
import io.asteria.reconciliation.domain.valueobject.ReconciliationRunId;
import io.asteria.reconciliation.infrastructure.persistence.dataobject.ReconciliationCaseDO;
import org.springframework.stereotype.Component;

/**
 * ReconciliationCase 与数据库行的显式转换；保留 String/TEXT JSON，不重复序列化。
 */
@Component
public class ReconciliationCasePersistenceConverter {
    /**
     * 将类型化 ID、枚举及 Money 拆分为现有数据库列类型。
     */
    public ReconciliationCaseDO toDataObject(ReconciliationCase domain) {
        return ReconciliationCaseDO.builder()
                .id(domain.getCaseId().value())
                .ruleCode(domain.getRuleCode())
                .businessKey(domain.getBusinessKey())
                .differenceType(domain.getDifferenceType().name())
                .runId(domain.getRunId().value())
                .matchResultId(domain.getMatchResultId().value())
                .status(domain.getStatus().name())
                .assignee(domain.getAssignee())
                .details(domain.getDetails())
                .resolution(domain.getResolution())
                .closedAt(domain.getClosedAt())
                .version(domain.getVersion())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }

    /**
     * 从持久化数据恢复，重新执行 Builder 的基本领域约束。
     */
    public ReconciliationCase toDomain(ReconciliationCaseDO dataObject) {
        return ReconciliationCase.builder()
                .caseId(ReconciliationCaseId.of(dataObject.getId()))
                .ruleCode(dataObject.getRuleCode())
                .businessKey(dataObject.getBusinessKey())
                .differenceType(ReconciliationDifferenceType.valueOf(dataObject.getDifferenceType()))
                .runId(ReconciliationRunId.of(dataObject.getRunId()))
                .matchResultId(ReconciliationMatchResultId.of(dataObject.getMatchResultId()))
                .status(ReconciliationCaseStatus.valueOf(dataObject.getStatus()))
                .assignee(dataObject.getAssignee())
                .details(dataObject.getDetails())
                .resolution(dataObject.getResolution())
                .closedAt(dataObject.getClosedAt())
                .version(dataObject.getVersion())
                .createdAt(dataObject.getCreatedAt())
                .updatedAt(dataObject.getUpdatedAt())
                .build();
    }
}
