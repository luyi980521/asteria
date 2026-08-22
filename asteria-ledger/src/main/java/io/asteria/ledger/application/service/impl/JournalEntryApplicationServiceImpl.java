package io.asteria.ledger.application.service.impl;

import io.asteria.common.application.port.DistributedIdGenerator;
import io.asteria.common.util.JsonUtils;
import io.asteria.ledger.application.assembler.PostingAssembler;
import io.asteria.ledger.application.command.CreateAndPostJournalEntryCommand;
import io.asteria.ledger.application.service.JournalEntryApplicationService;
import io.asteria.ledger.domain.entity.JournalEntry;
import io.asteria.ledger.domain.entity.Posting;
import io.asteria.ledger.domain.error.LedgerErrorCode;
import io.asteria.ledger.domain.exception.LedgerDomainException;
import io.asteria.ledger.domain.repository.JournalEntryRepository;
import io.asteria.ledger.domain.valueobject.JournalEntryId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * 记账凭证功能接口定义实现类
 * */
@Slf4j
@Service
public class JournalEntryApplicationServiceImpl implements JournalEntryApplicationService {

    @Autowired
    private JournalEntryRepository journalEntryRepository;

    @Autowired
    private PostingAssembler postingAssembler;

    @Autowired
    private DistributedIdGenerator distributedIdGenerator;

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

        List<Posting> postings = command.postings().stream()
                .map(pc -> postingAssembler.toEntity(pc))
                .toList();

        JournalEntryId journalEntryId = JournalEntryId.of(distributedIdGenerator.nextId());
        JournalEntry journalEntry = JournalEntry.create(journalEntryId, postings, command.reference());
        journalEntry.post(Instant.now());
        journalEntryRepository.insert(journalEntry);

        log.info("Journal entry create and post successfully, journalEntryId: {}, eventId: {}, postingCount: {}",
                journalEntryId, eventId, postings.size());
        return journalEntryId;
    }
}
