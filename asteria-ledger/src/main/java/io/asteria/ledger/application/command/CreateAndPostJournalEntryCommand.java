package io.asteria.ledger.application.command;

import io.asteria.ledger.domain.error.LedgerErrorCode;
import io.asteria.ledger.domain.exception.LedgerDomainException;
import io.asteria.ledger.domain.valueobject.JournalReference;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;

public record CreateAndPostJournalEntryCommand(
        JournalReference reference,
        List<PostingCommand> postings
) {

    public CreateAndPostJournalEntryCommand {

        if (reference() == null) {
            throw new LedgerDomainException(LedgerErrorCode.INVALID_PARAMS);
        }

        if (CollectionUtils.isEmpty(postings)) {
            throw new LedgerDomainException(LedgerErrorCode.INVALID_PARAMS);
        }
    }
}