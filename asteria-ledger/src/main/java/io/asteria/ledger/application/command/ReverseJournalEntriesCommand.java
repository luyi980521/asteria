package io.asteria.ledger.application.command;

import io.asteria.ledger.domain.error.LedgerErrorCode;
import io.asteria.ledger.domain.exception.LedgerDomainException;

import java.util.List;

/**
 * 批量冲正请求参数定义
 * */
public record ReverseJournalEntriesCommand(
        List<String> eventIds,
        String reason
) {

    public ReverseJournalEntriesCommand {

        if (eventIds == null || eventIds.isEmpty() || eventIds.size() > 1000
                || eventIds.stream().anyMatch(id -> id == null || id.isBlank())
                || reason == null || reason.isBlank() || reason.length() > 512) {
            throw new LedgerDomainException(LedgerErrorCode.INVALID_PARAMS);
        }
    }
}
