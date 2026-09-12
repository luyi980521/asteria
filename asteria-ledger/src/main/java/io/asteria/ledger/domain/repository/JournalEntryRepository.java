package io.asteria.ledger.domain.repository;

import io.asteria.ledger.domain.entity.JournalEntry;
import io.asteria.ledger.domain.valueobject.JournalEntryId;

import java.util.Optional;

public interface JournalEntryRepository {

    void insert(JournalEntry journalEntry);

    void update(JournalEntry journalEntry);

    Optional<JournalEntry> findById(JournalEntryId journalEntryId);

    boolean existsByEventId(String eventId);

    /** Loads and locks the entry within the caller transaction. */
    Optional<JournalEntry> findByEventIdForUpdate(String eventId);
}
