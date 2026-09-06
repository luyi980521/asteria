package io.asteria.ledger.infrastructure.persistence.converter;

import io.asteria.common.domain.valueobject.CurrencyCode;
import io.asteria.common.domain.valueobject.Money;
import io.asteria.ledger.domain.entity.JournalEntry;
import io.asteria.ledger.domain.entity.Posting;
import io.asteria.ledger.domain.enums.DebitCredit;
import io.asteria.ledger.domain.enums.JournalEntryStatus;
import io.asteria.ledger.domain.error.LedgerErrorCode;
import io.asteria.ledger.domain.exception.LedgerDomainException;
import io.asteria.ledger.domain.valueobject.*;
import io.asteria.ledger.infrastructure.persistence.dataobject.JournalEntryDO;
import io.asteria.ledger.infrastructure.persistence.dataobject.PostingDO;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.IntStream;

@Component
public class JournalEntryPersistenceConverter {

    public JournalEntryDO toJournalEntryDO(JournalEntry journalEntry) {
        if (journalEntry == null || journalEntry.getJournalEntryId() == null
                || journalEntry.getReference() == null || journalEntry.getStatus() == null) {
            throw new LedgerDomainException(LedgerErrorCode.INVALID_PARAMS);
        }

        JournalEntryDO journalEntryDO = new JournalEntryDO();
        journalEntryDO.setId(journalEntry.getJournalEntryId().value());
        journalEntryDO.setSourceType(journalEntry.getReference().sourceType());
        journalEntryDO.setSourceId(journalEntry.getReference().sourceId());
        journalEntryDO.setEventType(journalEntry.getReference().eventType());
        journalEntryDO.setEventId(journalEntry.getReference().eventId());
        journalEntryDO.setStatus(journalEntry.getStatus().name());
        journalEntryDO.setPostedAt(toDate(journalEntry.getPostedAt()));
        journalEntryDO.setOriginalJournalEntryId(toValue(journalEntry.getOriginalJournalEntryId()));
        journalEntryDO.setReversingJournalEntryId(toValue(journalEntry.getReversingJournalEntryId()));
        journalEntryDO.setReversalReason(journalEntry.getReversalReason());
        journalEntryDO.setReversedAt(toDate(journalEntry.getReversedAt()));
        return journalEntryDO;
    }

    public List<PostingDO> toPostingDOList(JournalEntry journalEntry) {
        if (journalEntry == null || journalEntry.getJournalEntryId() == null
                || CollectionUtils.isEmpty(journalEntry.getPostings())) {
            throw new LedgerDomainException(LedgerErrorCode.INVALID_PARAMS);
        }

        Long journalEntryId = journalEntry.getJournalEntryId().value();
        return IntStream.range(0, journalEntry.getPostings().size())
                .mapToObj(index -> toPostingDO(journalEntry.getPostings().get(index), journalEntryId, index + 1))
                .toList();
    }

    public JournalEntry toDomain(JournalEntryDO journalEntryDO, List<PostingDO> postingDOList) {
        if (journalEntryDO == null || CollectionUtils.isEmpty(postingDOList)) {
            throw new LedgerDomainException(LedgerErrorCode.INVALID_PARAMS);
        }

        JournalReference reference = new JournalReference(
                journalEntryDO.getSourceType(),
                journalEntryDO.getSourceId(),
                journalEntryDO.getEventType(),
                journalEntryDO.getEventId());

        List<Posting> postings = postingDOList.stream()
                .sorted(Comparator.comparing(PostingDO::getSequenceNo))
                .map(this::toPostingDomain)
                .toList();

        return JournalEntry.reconstitute(
                toJournalEntryId(journalEntryDO.getId()),
                postings,
                valueOfStatus(journalEntryDO.getStatus()),
                reference,
                toInstant(journalEntryDO.getPostedAt()),
                toNullableJournalEntryId(journalEntryDO.getOriginalJournalEntryId()),
                toNullableJournalEntryId(journalEntryDO.getReversingJournalEntryId()),
                journalEntryDO.getReversalReason(),
                toInstant(journalEntryDO.getReversedAt()));
    }

    public Posting toPostingDomain(PostingDO postingDO) {
        if (postingDO == null) {
            throw new LedgerDomainException(LedgerErrorCode.INVALID_PARAMS);
        }

        return Posting.reconstitute(
                toPostingId(postingDO.getId()),
                toLedgerAccountId(postingDO.getLedgerAccountId()),
                toMoney(postingDO.getAmount(), postingDO.getCurrency()),
                valueOfDirection(postingDO.getDirection()));
    }

    private PostingDO toPostingDO(Posting posting, Long journalEntryId, int sequenceNo) {
        if (posting == null || posting.getPostingId() == null || posting.getLedgerAccountId() == null
                || posting.getMoney() == null || posting.getDirection() == null) {
            throw new LedgerDomainException(LedgerErrorCode.INVALID_PARAMS);
        }

        PostingDO postingDO = new PostingDO();
        postingDO.setId(posting.getPostingId().value());
        postingDO.setJournalEntryId(journalEntryId);
        postingDO.setLedgerAccountId(posting.getLedgerAccountId().value());
        postingDO.setAmount(posting.getMoney().amount());
        postingDO.setCurrency(posting.getMoney().currency().value());
        postingDO.setDirection(posting.getDirection().name());
        postingDO.setSequenceNo(sequenceNo);
        return postingDO;
    }

    private Money toMoney(BigDecimal amount, String currencyCode) {
        if (StringUtils.isBlank(currencyCode)) {
            throw new LedgerDomainException(LedgerErrorCode.INVALID_PARAMS);
        }

        try {
            return Money.of(amount, CurrencyCode.of(currencyCode));
        } catch (IllegalArgumentException exception) {
            throw new LedgerDomainException(LedgerErrorCode.INVALID_PARAMS, exception);
        }
    }

    private JournalEntryStatus valueOfStatus(String status) {
        if (StringUtils.isBlank(status)) {
            throw new LedgerDomainException(LedgerErrorCode.INVALID_PARAMS);
        }

        try {
            return JournalEntryStatus.valueOf(status);
        } catch (IllegalArgumentException exception) {
            throw new LedgerDomainException(LedgerErrorCode.INVALID_PARAMS, exception);
        }
    }

    private DebitCredit valueOfDirection(String direction) {
        if (StringUtils.isBlank(direction)) {
            throw new LedgerDomainException(LedgerErrorCode.INVALID_PARAMS);
        }

        try {
            return DebitCredit.valueOf(direction);
        } catch (IllegalArgumentException exception) {
            throw new LedgerDomainException(LedgerErrorCode.INVALID_PARAMS, exception);
        }
    }

    private JournalEntryId toJournalEntryId(Long value) {
        if (value == null) {
            throw new LedgerDomainException(LedgerErrorCode.INVALID_PARAMS);
        }
        return JournalEntryId.of(value);
    }

    private JournalEntryId toNullableJournalEntryId(Long value) {
        return value == null ? null : toJournalEntryId(value);
    }

    private PostingId toPostingId(Long value) {
        if (value == null) {
            throw new LedgerDomainException(LedgerErrorCode.INVALID_PARAMS);
        }
        return PostingId.of(value);
    }

    private LedgerAccountId toLedgerAccountId(Long value) {
        if (value == null) {
            throw new LedgerDomainException(LedgerErrorCode.INVALID_PARAMS);
        }
        return LedgerAccountId.of(value);
    }

    private Long toValue(JournalEntryId value) {
        return value == null ? null : value.value();
    }

    private Date toDate(Instant value) {
        return value == null ? null : Date.from(value);
    }

    private Instant toInstant(Date value) {
        return value == null ? null : value.toInstant();
    }
}
