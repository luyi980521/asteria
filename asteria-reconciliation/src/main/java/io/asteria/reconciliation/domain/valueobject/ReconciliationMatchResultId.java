package io.asteria.reconciliation.domain.valueobject;

import io.asteria.reconciliation.domain.error.ReconciliationErrorCode;
import io.asteria.reconciliation.domain.exception.ReconciliationDomainException;

/**
 * MatchResult 的类型化标识；由应用使用 DistributedIdGenerator 生成正数雪花 ID。
 */
public record ReconciliationMatchResultId(Long value) {
    /**
     * 拒绝空值及非正数，避免不同对象之间误用标识。
     */
    public ReconciliationMatchResultId {
        if (value == null || value <= 0) {
            throw new ReconciliationDomainException(ReconciliationErrorCode.INVALID_PARAMS);
        }
    }

    /**
     * 从应用生成或持久化读取的雪花 ID 恢复标识。
     */
    public static ReconciliationMatchResultId of(Long value) { return new ReconciliationMatchResultId(value); }
}
