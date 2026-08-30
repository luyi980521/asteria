package io.asteria.ledger.application.assembler;

import io.asteria.ledger.application.command.CreateAndPostJournalEntryCommand;
import io.asteria.ledger.application.command.PostingCommand;
import io.asteria.ledger.application.message.PaymentCapturedMessage;
import io.asteria.ledger.application.model.PaymentCapturedLedgerAccounts;
import io.asteria.ledger.application.resolver.PaymentLedgerAccountResolver;
import io.asteria.ledger.domain.enums.DebitCredit;
import io.asteria.ledger.domain.valueobject.JournalReference;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 支付捕获事件记账转换器
 * */
@Component
@RequiredArgsConstructor
public class PaymentCapturedLedgerAssembler {

    private final PaymentLedgerAccountResolver paymentLedgerAccountResolver;

    /**
     * 组装支付捕获成功记账命令
     */
    public CreateAndPostJournalEntryCommand toCreateAndPostCommand(PaymentCapturedMessage message) {

        JournalReference reference = JournalReference.builder()
                .sourceType("PAYMENT")
                .sourceId("PAYMENT" + message.paymentId())
                .eventType("PAYMENT_CAPTURED")
                .eventId(message.eventId())
                .build();

        PaymentCapturedLedgerAccounts ledgerAccounts
                = paymentLedgerAccountResolver.resolve(message.amount().currency().getSymbol());

        PostingCommand debitPosting = PostingCommand.builder()
                .ledgerAccountId(ledgerAccounts.creditAccountId())
                .direction(DebitCredit.DEBIT)
                .money(message.amount())
                .build();

        PostingCommand creditPosting = PostingCommand.builder()
                .ledgerAccountId(ledgerAccounts.debitAccountId())
                .direction(DebitCredit.CREDIT)
                .money(message.amount())
                .build();

        return CreateAndPostJournalEntryCommand.builder()
                .reference(reference)
                .postings(List.of(debitPosting, creditPosting))
                .build();
    }
}
