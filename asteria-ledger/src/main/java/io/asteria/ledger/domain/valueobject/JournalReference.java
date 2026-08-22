package io.asteria.ledger.domain.valueobject;

import io.asteria.ledger.domain.error.LedgerErrorCode;
import io.asteria.ledger.domain.exception.LedgerDomainException;
import org.apache.commons.lang3.StringUtils;

/**
 * 用于确认记账凭证是由哪一个业务事件触发产生
 * */
public record JournalReference(

        /* 业务对象类型，例如 PAYMENT */
        String sourceType,

        /* 业务对象 ID，例如 pay_123 */
        String sourceId,

        /* 发生了什么，例如 PAYMENT_CAPTURED */
        String eventType,

        /* 本次事件的唯一 ID，用于幂等 */
        String eventId
) {

    public JournalReference {
        if (StringUtils.isBlank(sourceType)) {
            throw new LedgerDomainException(LedgerErrorCode.SOURCE_TYPE_REQUIRED);
        }

        if (StringUtils.isBlank(sourceId)) {
            throw new LedgerDomainException(LedgerErrorCode.SOURCE_ID_REQUIRED);
        }

        if (StringUtils.isBlank(eventType)) {
            throw new LedgerDomainException(LedgerErrorCode.EVENT_TYPE_REQUIRED);
        }

        if (StringUtils.isBlank(eventId)) {
            throw new LedgerDomainException(LedgerErrorCode.EVENT_ID_REQUIRED);
        }
    }
}