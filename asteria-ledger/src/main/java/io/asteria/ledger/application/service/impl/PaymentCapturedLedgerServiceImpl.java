package io.asteria.ledger.application.service.impl;

import io.asteria.ledger.application.assembler.PaymentCapturedLedgerAssembler;
import io.asteria.ledger.application.command.CreateAndPostJournalEntryCommand;
import io.asteria.ledger.application.message.PaymentCapturedMessage;
import io.asteria.ledger.application.service.JournalEntryApplicationService;
import io.asteria.ledger.application.service.PaymentCapturedLedgerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 支付捕获事件账本功能接口定义实现类
 * */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentCapturedLedgerServiceImpl implements PaymentCapturedLedgerService {

    private final PaymentCapturedLedgerAssembler paymentCapturedLedgerAssembler;
    private final JournalEntryApplicationService journalEntryApplicationService;

    /**
     * 处理支付捕获事件
     */
    @Override
    public void handle(PaymentCapturedMessage message) {

        CreateAndPostJournalEntryCommand command =
                paymentCapturedLedgerAssembler.toCreateAndPostCommand(message);
        journalEntryApplicationService.createAndPost(command);

        log.info("Payment captured event posted to ledger, eventId: {}, paymentId: {}",
                message.eventId(), message.paymentId());
    }
}
