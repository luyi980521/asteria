package io.asteria.ledger.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import io.asteria.ledger.domain.entity.JournalEntry;
import io.asteria.ledger.domain.error.LedgerErrorCode;
import io.asteria.ledger.domain.exception.LedgerDomainException;
import io.asteria.ledger.domain.repository.JournalEntryRepository;
import io.asteria.ledger.domain.valueobject.JournalEntryId;
import io.asteria.ledger.infrastructure.persistence.converter.JournalEntryPersistenceConverter;
import io.asteria.ledger.infrastructure.persistence.dataobject.JournalEntryDO;
import io.asteria.ledger.infrastructure.persistence.dataobject.PostingDO;
import io.asteria.ledger.infrastructure.persistence.mapper.JournalEntryMapper;
import io.asteria.ledger.infrastructure.persistence.mapper.PostingMapper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class JournalEntryRepositoryImpl implements JournalEntryRepository {

    private final JournalEntryMapper journalEntryMapper;
    private final PostingMapper postingMapper;
    private final JournalEntryPersistenceConverter converter;

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
