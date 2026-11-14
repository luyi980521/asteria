package io.asteria.ledger.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import io.asteria.ledger.domain.entity.JournalEntry;
import io.asteria.ledger.domain.error.LedgerErrorCode;
import io.asteria.ledger.domain.exception.LedgerDomainException;
import io.asteria.ledger.domain.repository.JournalEntryRepository;
import io.asteria.ledger.domain.valueobject.JournalEntryId;
import io.asteria.ledger.domain.valueobject.JournalEntrySnapshot;
import io.asteria.ledger.domain.valueobject.PostingSnapshot;
import io.asteria.ledger.infrastructure.persistence.converter.JournalEntryPersistenceConverter;
import io.asteria.ledger.infrastructure.persistence.dataobject.JournalEntryDO;
import io.asteria.ledger.infrastructure.persistence.dataobject.PostingDO;
import io.asteria.ledger.infrastructure.persistence.mapper.JournalEntryMapper;
import io.asteria.ledger.infrastructure.persistence.mapper.PostingMapper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;
import java.time.Instant;

@Repository
@RequiredArgsConstructor
public class JournalEntryRepositoryImpl implements JournalEntryRepository {

    private final JournalEntryMapper journalEntryMapper;
    private final PostingMapper postingMapper;
    private final JournalEntryPersistenceConverter converter;

    @Override
    public List<JournalEntrySnapshot> findInPostedWindow(Instant start, Instant end, long afterId, int limit) {
        if (start == null || end == null || !start.isBefore(end) || afterId < 0 || limit < 1 || limit > 10000) {
            throw new IllegalArgumentException("Invalid journal export window or page");
        }
        List<JournalEntryDO> entries = journalEntryMapper.selectList(new LambdaQueryWrapper<JournalEntryDO>()
                .ge(JournalEntryDO::getPostedAt, start)
                .lt(JournalEntryDO::getPostedAt, end)
                .gt(JournalEntryDO::getId, afterId).orderByAsc(JournalEntryDO::getId).last("LIMIT " + limit));
        if (entries.isEmpty()) return List.of();
        var postings = postingRows(entries.stream().map(JournalEntryDO::getId).toList());
        return entries.stream().map(entry -> new JournalEntrySnapshot(
                converter.toDomain(entry, postings.getOrDefault(entry.getId(), List.of())),
                entry.getVersion(), entry.getCreatedAt().toInstant(), entry.getUpdatedAt().toInstant())).toList();
    }

    @Override
    public Map<JournalEntryId, List<PostingSnapshot>> findPostingsByJournalEntryIds(List<JournalEntryId> ids) {
        if (ids == null || ids.isEmpty()) return Map.of();
        var result = new LinkedHashMap<JournalEntryId, List<PostingSnapshot>>();
        postingRows(ids.stream().map(JournalEntryId::value).distinct().toList()).forEach((id, rows) ->
                result.put(JournalEntryId.of(id), rows.stream().map(row -> new PostingSnapshot(
                        JournalEntryId.of(id), row.getSequenceNo(), converter.toPostingDomain(row))).toList()));
        return Collections.unmodifiableMap(result);
    }

    /**
     * 同模块内部批量读取行，避免导出一页时出现 N+1 查询。
     */
    private Map<Long, List<PostingDO>> postingRows(List<Long> ids) {
        var result = new LinkedHashMap<Long, List<PostingDO>>();
        for (int offset = 0; offset < ids.size(); offset += 1000) {
            var rows = postingMapper.selectList(new LambdaQueryWrapper<PostingDO>()
                    .in(PostingDO::getJournalEntryId, ids.subList(offset, Math.min(offset + 1000, ids.size())))
                    .orderByAsc(PostingDO::getJournalEntryId, PostingDO::getSequenceNo));
            for (PostingDO row : rows) result.computeIfAbsent(row.getJournalEntryId(), key -> new ArrayList<>()).add(row);
        }
        return result;
    }

    @Override
    public void insert(JournalEntry journalEntry) {
        JournalEntryDO journalEntryDO = converter.toJournalEntryDO(journalEntry);
        journalEntryMapper.insert(journalEntryDO);

        List<PostingDO> postingDOList = converter.toPostingDOList(journalEntry);
        for (PostingDO postingDO : postingDOList) {
            postingMapper.insert(postingDO);
        }
    }

    @Override
    public void update(JournalEntry journalEntry) {
        JournalEntryDO journalEntryDO = converter.toJournalEntryDO(journalEntry);

        LambdaUpdateWrapper<JournalEntryDO> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper
                .eq(JournalEntryDO::getId, journalEntryDO.getId())
                .set(JournalEntryDO::getStatus, journalEntryDO.getStatus())
                .set(JournalEntryDO::getPostedAt, journalEntryDO.getPostedAt())
                .set(JournalEntryDO::getReversingJournalEntryId,
                        journalEntryDO.getReversingJournalEntryId())
                .set(JournalEntryDO::getReversalReason, journalEntryDO.getReversalReason())
                .set(JournalEntryDO::getReversedAt, journalEntryDO.getReversedAt())
                .setSql("version = version + 1");

        journalEntryMapper.update(new JournalEntryDO(), updateWrapper);
    }

    @Override
    public Optional<JournalEntry> findById(JournalEntryId journalEntryId) {
        if (journalEntryId == null) {
            throw new LedgerDomainException(LedgerErrorCode.NULL_ARGUMENT);
        }

        Long id = journalEntryId.value();
        JournalEntryDO journalEntryDO = journalEntryMapper.selectById(id);
        if (journalEntryDO == null) {
            return Optional.empty();
        }

        LambdaQueryWrapper<PostingDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper
                .eq(PostingDO::getJournalEntryId, id)
                .orderByAsc(PostingDO::getSequenceNo);
        List<PostingDO> postingDOList = postingMapper.selectList(queryWrapper);

        return Optional.of(converter.toDomain(journalEntryDO, postingDOList));
    }

    @Override
    public Optional<JournalEntry> findByEventIdForUpdate(String eventId) {
        JournalEntryDO entry = journalEntryMapper.selectOne(new LambdaQueryWrapper<JournalEntryDO>()
                .eq(JournalEntryDO::getEventId, eventId).last("FOR UPDATE"));
        return entry == null ? Optional.empty() : findById(JournalEntryId.of(entry.getId()));
    }

    @Override
    public boolean existsByEventId(String eventId) {
        if (StringUtils.isBlank(eventId)) {
            throw new LedgerDomainException(LedgerErrorCode.INVALID_PARAMS);
        }

        LambdaQueryWrapper<JournalEntryDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper
                .select(JournalEntryDO::getId)
                .eq(JournalEntryDO::getEventId, eventId)
                .last("LIMIT 1");
        return journalEntryMapper.selectCount(queryWrapper) > 0;
    }
}
