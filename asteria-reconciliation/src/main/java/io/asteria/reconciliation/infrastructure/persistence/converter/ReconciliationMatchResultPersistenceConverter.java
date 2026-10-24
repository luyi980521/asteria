package io.asteria.reconciliation.infrastructure.persistence.converter;

import io.asteria.common.domain.valueobject.CurrencyCode;
import io.asteria.common.domain.valueobject.Money;
import io.asteria.reconciliation.domain.entity.ReconciliationMatchResult;
import io.asteria.reconciliation.domain.enums.ReconciliationDifferenceType;
import io.asteria.reconciliation.domain.enums.ReconciliationMatchStatus;
import io.asteria.reconciliation.domain.valueobject.ReconciliationMatchResultId;
import io.asteria.reconciliation.domain.valueobject.ReconciliationRunId;
import io.asteria.reconciliation.infrastructure.persistence.dataobject.ReconciliationMatchResultDO;
import org.springframework.stereotype.Component;

/**
 * ReconciliationMatchResult 与数据库行的显式转换；保留 String/TEXT JSON，不重复序列化。
 */
@Component
public class ReconciliationMatchResultPersistenceConverter {
    /**
     * 将类型化 ID、枚举及 Money 拆分为现有数据库列类型。
     */
    public ReconciliationMatchResultDO toDataObject(ReconciliationMatchResult domain) {
        return ReconciliationMatchResultDO.builder()
                .id(domain.getMatchResultId().value())
                .runId(domain.getRunId().value())
                .ruleCode(domain.getRuleCode())
                .businessKey(domain.getBusinessKey())
                .status(domain.getStatus().name())
                .differenceType(domain.getDifferenceType() == null ? null : domain.getDifferenceType().name())
                .expectedAmount(domain.getExpectedAmount() == null ? null : domain.getExpectedAmount().amount())
                .expectedCurrency(domain.getExpectedAmount() == null ? null : domain.getExpectedAmount().currency().value())
                .actualAmount(domain.getActualAmount() == null ? null : domain.getActualAmount().amount())
                .actualCurrency(domain.getActualAmount() == null ? null : domain.getActualAmount().currency().value())
                .sourceRecordIds(domain.getSourceRecordIds())
                .details(domain.getDetails())
                .createdAt(domain.getCreatedAt())
                .build();
    }

    /**
     * 从持久化数据恢复，重新执行 Builder 的基本领域约束。
     */
    public ReconciliationMatchResult toDomain(ReconciliationMatchResultDO dataObject) {
        return ReconciliationMatchResult.builder()
                .matchResultId(ReconciliationMatchResultId.of(dataObject.getId()))
                .runId(ReconciliationRunId.of(dataObject.getRunId()))
                .ruleCode(dataObject.getRuleCode())
                .businessKey(dataObject.getBusinessKey())
                .status(ReconciliationMatchStatus.valueOf(dataObject.getStatus()))
                .differenceType(dataObject.getDifferenceType() == null ? null : ReconciliationDifferenceType.valueOf(dataObject.getDifferenceType()))
                .expectedAmount(dataObject.getExpectedAmount() == null ? null : Money.ofNonNegative(dataObject.getExpectedAmount(), CurrencyCode.of(dataObject.getExpectedCurrency())))
                .actualAmount(dataObject.getActualAmount() == null ? null : Money.ofNonNegative(dataObject.getActualAmount(), CurrencyCode.of(dataObject.getActualCurrency())))
                .sourceRecordIds(dataObject.getSourceRecordIds())
                .details(dataObject.getDetails())
                .createdAt(dataObject.getCreatedAt())
                .build();
    }
}
