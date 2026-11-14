package io.asteria.ledger.domain.repository;

import io.asteria.ledger.domain.entity.JournalEntry;
import io.asteria.ledger.domain.valueobject.JournalEntryId;
import io.asteria.ledger.domain.valueobject.JournalEntrySnapshot;
import io.asteria.ledger.domain.valueobject.PostingSnapshot;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface JournalEntryRepository {

    /**
     * 按 postedAt 的 [start, end) 分页，ID 升序；afterId 首屏为 0，limit 为 1..10000。
     * 草稿无 postedAt，不属于入账业务窗口；调用方负责快照隔离或来源完成水位。
     */
    List<JournalEntrySnapshot> findInPostedWindow(Instant start, Instant end, long afterId, int limit);

    /**
     * 批量读取各凭证分录，按凭证内 sequenceNo 排序；空集合返回空 Map。
     */
    Map<JournalEntryId, List<PostingSnapshot>> findPostingsByJournalEntryIds(List<JournalEntryId> journalEntryIds);

    void insert(JournalEntry journalEntry);

    void update(JournalEntry journalEntry);

    Optional<JournalEntry> findById(JournalEntryId journalEntryId);

    boolean existsByEventId(String eventId);

    /**
     * Loads and locks the entry within the caller transaction.
     */
    Optional<JournalEntry> findByEventIdForUpdate(String eventId);
}
