package io.asteria.ledger.application.service.impl;

import io.asteria.common.application.port.DistributedIdGenerator;
import io.asteria.ledger.application.response.ReverseJournalEntriesResponse;
import io.asteria.common.util.JsonUtils;
import io.asteria.ledger.application.assembler.PostingAssembler;
import io.asteria.ledger.application.command.CreateAndPostJournalEntryCommand;
import io.asteria.ledger.application.command.ReverseJournalEntriesCommand;
import io.asteria.ledger.application.service.JournalEntryApplicationService;
import io.asteria.ledger.application.service.LedgerAccountApplicationService;
import io.asteria.ledger.domain.entity.JournalEntry;
import io.asteria.ledger.domain.entity.Posting;
import io.asteria.ledger.domain.enums.JournalEntryReverseStatus;
import io.asteria.ledger.domain.error.LedgerErrorCode;
import io.asteria.ledger.domain.exception.LedgerDomainException;
import io.asteria.ledger.domain.repository.JournalEntryRepository;
import io.asteria.ledger.domain.valueobject.JournalEntryId;
import io.asteria.ledger.domain.valueobject.PostingId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 记账凭证功能接口定义实现类
 * */
@Slf4j
@Service
public class JournalEntryApplicationServiceImpl implements JournalEntryApplicationService {

    private final TransactionTemplate reversalTransaction;

    private final JournalEntryRepository journalEntryRepository;

    private final PostingAssembler postingAssembler;

    private final DistributedIdGenerator distributedIdGenerator;

    private final LedgerAccountApplicationService ledgerAccountApplicationService;

    public JournalEntryApplicationServiceImpl(PlatformTransactionManager transactionManager,
                                              JournalEntryRepository journalEntryRepository,
                                              PostingAssembler postingAssembler,
                                              DistributedIdGenerator distributedIdGenerator,
                                              LedgerAccountApplicationService ledgerAccountApplicationService) {
        this.reversalTransaction = new TransactionTemplate(transactionManager);
        this.reversalTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.journalEntryRepository = journalEntryRepository;
        this.postingAssembler = postingAssembler;
        this.distributedIdGenerator = distributedIdGenerator;
        this.ledgerAccountApplicationService = ledgerAccountApplicationService;
    }

    /**
     * 创建并入账
     *
     * 流程：
     * 1. 参数检查
     * 2. 事件幂等检查
     * 3. 创建 Posting
     * 4. 创建 JournalEntry
     * 5. 入账
     * 6. 保存
     * 7. 返回 ID
     *
     * @param command {@link CreateAndPostJournalEntryCommand}
     * @return {@link JournalEntryId}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public JournalEntryId createAndPost(CreateAndPostJournalEntryCommand command) {

        String eventId = command.reference().eventId();
        boolean isExists = journalEntryRepository.existsByEventId(eventId);
        if (isExists) {
            log.warn("Journal entry already exists: {}", JsonUtils.toJson(command.reference()));
            throw new LedgerDomainException(LedgerErrorCode.DUPLICATE_LEDGER_EVENT);
        }
        ledgerAccountApplicationService.validatePostable(command.postings());

        List<Posting> postings = command.postings().stream()
                .map(postingAssembler::toEntity)
                .toList();

        JournalEntryId journalEntryId = JournalEntryId.of(distributedIdGenerator.nextId());
        JournalEntry journalEntry = JournalEntry.create(journalEntryId, postings, command.reference());
        journalEntry.post(Instant.now());
        journalEntryRepository.insert(journalEntry);

        log.info("Journal entry create and post successfully, journalEntryId: {}, eventId: {}, postingCount: {}",
                journalEntryId.value(), eventId, postings.size());
        return journalEntryId;
    }

    /**
     * 通过 eventId 批量冲正
     */
    @Override
    public ReverseJournalEntriesResponse reverseByEventIds(ReverseJournalEntriesCommand command) {

        List<ReverseJournalEntriesResponse.Result> results = new ArrayList<>();
        for (String eventId : command.eventIds()) {
            try {
                results.add(reversalTransaction.execute(status -> reverseOne(eventId, command.reason())));
            } catch (LedgerDomainException exception) {
                log.warn("Journal entry reversal rejected, eventId: {}, code: {}, message: {}",
                        eventId, exception.errorCode().code(), exception.getMessage());
                results.add(
                        new ReverseJournalEntriesResponse.Result(
                                eventId, JournalEntryReverseStatus.FAILED,
                                exception.errorCode().code(),
                                exception.getMessage()
                        )
                );
            } catch (RuntimeException exception) {
                log.error("Journal entry reversal failed, eventId: {}, message: {}",
                        eventId, exception.getMessage(), exception);
                results.add(new ReverseJournalEntriesResponse.Result(
                        eventId, JournalEntryReverseStatus.FAILED,
                        "SYSTEM_ERROR",
                        "Journal entry reversal failed"
                ));
            }
        }
        return new ReverseJournalEntriesResponse(results);
    }

    private ReverseJournalEntriesResponse.Result reverseOne(String eventId, String reason) {

        Optional<JournalEntry> original = journalEntryRepository.findByEventIdForUpdate(eventId);
        if (original.isEmpty()) {
            log.warn("Journal entry not found for reversal, eventId: {}", eventId);
            return new ReverseJournalEntriesResponse.Result(
                    eventId,
                    JournalEntryReverseStatus.FAILED,
                    "JOURNAL_ENTRY_NOT_FOUND",
                    "Journal entry not found"
            );
        }
        JournalEntry entry = original.get();
        if (entry.getReversingJournalEntryId() != null) {
            log.info("Journal entry already reversed, eventId: {}, reversalJournalEntryId: {}",
                    eventId, entry.getReversingJournalEntryId().value());
            return new ReverseJournalEntriesResponse.Result(
                    eventId,
                    JournalEntryReverseStatus.ALREADY_REVERSED,
                    null,
                    null
            );
        }
        List<PostingId> postingIds = entry.getPostings()
                .stream()
                .map(posting -> PostingId.of(distributedIdGenerator.nextId()))
                .toList();
        JournalEntry reversal = entry.reverse(
                JournalEntryId.of(distributedIdGenerator.nextId()),
                postingIds,
                Instant.now(),
                reason
        );
        journalEntryRepository.insert(reversal);
        journalEntryRepository.update(entry);
        log.info("Journal entry reversed, eventId: {}, reversalId: {}", eventId, reversal.getJournalEntryId().value());
        return new ReverseJournalEntriesResponse.Result(
                eventId,
                JournalEntryReverseStatus.REVERSED,
                null,
                null
        );
    }
}
