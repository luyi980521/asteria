package io.asteria.reconciliation.infrastructure.persistence.converter;

import io.asteria.reconciliation.domain.entity.ReconciliationRun;
import io.asteria.reconciliation.domain.enums.ReconciliationRunStatus;
import io.asteria.reconciliation.domain.enums.ReconciliationType;
import io.asteria.reconciliation.domain.valueobject.ReconciliationRunId;
import io.asteria.reconciliation.infrastructure.persistence.dataobject.ReconciliationRunDO;
import org.springframework.stereotype.Component;

/**
 * ReconciliationRun 与数据库行的显式转换；保留 String/TEXT JSON，不重复序列化。
 */
@Component
public class ReconciliationRunPersistenceConverter {
    /**
     * 将类型化 ID、枚举及 Money 拆分为现有数据库列类型。
     */
    public ReconciliationRunDO toDataObject(ReconciliationRun domain) {
        return ReconciliationRunDO.builder()
                .id(domain.getRunId().value())
                .ruleCode(domain.getRuleCode())
                .ruleVersion(domain.getRuleVersion())
                .type(domain.getType().name())
                .ruleSnapshot(domain.getRuleSnapshot())
                .windowStart(domain.getWindowStart())
                .windowEnd(domain.getWindowEnd())
                .sourceCutoffAt(domain.getSourceCutoffAt())
                .status(domain.getStatus().name())
                .totalCount(domain.getTotalCount())
                .matchedCount(domain.getMatchedCount())
                .mismatchedCount(domain.getMismatchedCount())
                .missingCount(domain.getMissingCount())
                .pendingCount(domain.getPendingCount())
                .duplicatedCount(domain.getDuplicatedCount())
                .startedAt(domain.getStartedAt())
                .finishedAt(domain.getFinishedAt())
                .failureReason(domain.getFailureReason())
                .version(domain.getVersion())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }

    /**
     * 从持久化数据恢复，重新执行 Builder 的基本领域约束。
     */
    public ReconciliationRun toDomain(ReconciliationRunDO dataObject) {
        return ReconciliationRun.builder()
                .runId(ReconciliationRunId.of(dataObject.getId()))
                .ruleCode(dataObject.getRuleCode())
                .ruleVersion(dataObject.getRuleVersion())
                .type(ReconciliationType.valueOf(dataObject.getType()))
                .ruleSnapshot(dataObject.getRuleSnapshot())
                .windowStart(dataObject.getWindowStart())
                .windowEnd(dataObject.getWindowEnd())
                .sourceCutoffAt(dataObject.getSourceCutoffAt())
                .status(ReconciliationRunStatus.valueOf(dataObject.getStatus()))
                .totalCount(dataObject.getTotalCount())
                .matchedCount(dataObject.getMatchedCount())
                .mismatchedCount(dataObject.getMismatchedCount())
                .missingCount(dataObject.getMissingCount())
                .pendingCount(dataObject.getPendingCount())
                .duplicatedCount(dataObject.getDuplicatedCount())
                .startedAt(dataObject.getStartedAt())
                .finishedAt(dataObject.getFinishedAt())
                .failureReason(dataObject.getFailureReason())
                .version(dataObject.getVersion())
                .createdAt(dataObject.getCreatedAt())
                .updatedAt(dataObject.getUpdatedAt())
                .build();
    }
}
