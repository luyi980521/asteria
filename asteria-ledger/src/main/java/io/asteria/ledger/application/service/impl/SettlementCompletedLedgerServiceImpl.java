package io.asteria.ledger.application.service.impl;

import io.asteria.ledger.application.assembler.SettlementCompletedLedgerAssembler;
import io.asteria.ledger.application.command.CreateAndPostJournalEntryCommand;
import io.asteria.ledger.application.message.SettlementCompletedMessage;
import io.asteria.ledger.application.service.JournalEntryApplicationService;
import io.asteria.ledger.application.service.SettlementCompletedLedgerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 结算完成事件账本功能接口定义实现类
 * */
@Slf4j
@Service
@RequiredArgsConstructor
public class SettlementCompletedLedgerServiceImpl implements SettlementCompletedLedgerService {

    private final SettlementCompletedLedgerAssembler settlementCompletedLedgerAssembler;
    private final JournalEntryApplicationService journalEntryApplicationService;

    /**
     * 处理支付捕获事件
     */
    @Override
    public void handle(SettlementCompletedMessage message) {

        CreateAndPostJournalEntryCommand command = settlementCompletedLedgerAssembler.toCreateAndPostCommand(message);
        journalEntryApplicationService.createAndPost(command);
        log.info("Settlement completed event posted to ledger, eventId: {}, settlementId: {}",
                message.eventId(), message.settlementBatchId());
    }
}
