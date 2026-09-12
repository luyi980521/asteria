package io.asteria.ledger.application.service;

import io.asteria.ledger.application.response.ReverseJournalEntriesResponse;
import io.asteria.ledger.application.command.CreateAndPostJournalEntryCommand;
import io.asteria.ledger.application.command.ReverseJournalEntriesCommand;
import io.asteria.ledger.domain.valueobject.JournalEntryId;

/**
 * 记账凭证功能接口定义
 * */
public interface JournalEntryApplicationService {

    /**
     * 创建并入账
     * @param command {@link CreateAndPostJournalEntryCommand}
     * @return {@link JournalEntryId}
     * */
    JournalEntryId createAndPost(CreateAndPostJournalEntryCommand command);

    /**
     * 通过 eventId 批量冲正
     * */
    ReverseJournalEntriesResponse reverseByEventIds(ReverseJournalEntriesCommand command);
}
