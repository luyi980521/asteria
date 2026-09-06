package io.asteria.ledger.application.assembler;

import io.asteria.ledger.application.command.CreateAndPostJournalEntryCommand;
import io.asteria.ledger.application.command.PostingCommand;
import io.asteria.ledger.application.message.SettlementCompletedMessage;
import io.asteria.ledger.application.model.SettlementCompletedLedgerAccounts;
import io.asteria.ledger.application.resolver.SettlementLedgerAccountResolver;
import io.asteria.ledger.domain.constants.LedgerSourceIds;
import io.asteria.ledger.domain.enums.DebitCredit;
import io.asteria.ledger.domain.enums.LedgerEventType;
import io.asteria.ledger.domain.enums.LedgerSourceType;
import io.asteria.ledger.domain.valueobject.JournalReference;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 结算完成事件记账转换器
 * */
@Component
@RequiredArgsConstructor
public class SettlementCompletedLedgerAssembler {

    private final SettlementLedgerAccountResolver settlementLedgerAccountResolver;

    /**
     * 组装结算完成记账命令
     */
    public CreateAndPostJournalEntryCommand toCreateAndPostCommand(SettlementCompletedMessage message) {

        JournalReference reference = JournalReference.builder()
                .sourceType(LedgerSourceType.SETTLEMENT_BATCH.name())
                .sourceId(LedgerSourceIds.settlementBatch(message.settlementBatchId()))
                .eventType(LedgerEventType.SETTLEMENT_COMPLETED.name())
                .eventId(message.eventId())
                .build();

        SettlementCompletedLedgerAccounts ledgerAccounts
                = settlementLedgerAccountResolver.resolve(message.grossAmount().currency().getSymbol());

        List<PostingCommand> postings = new ArrayList<>();
        PostingCommand cashDebitPosting = PostingCommand.builder()
                .ledgerAccountId(ledgerAccounts.cashAccountId())
                .direction(DebitCredit.DEBIT)
                .money(message.netAmount())
                .build();
        postings.add(cashDebitPosting);

        if (message.feeAmount().amount().compareTo(BigDecimal.ZERO) > 0) {
            PostingCommand feeExpenseDebitPosting = PostingCommand.builder()
                    .ledgerAccountId(ledgerAccounts.processingFeeExpenseAccountId())
                    .direction(DebitCredit.DEBIT)
                    .money(message.feeAmount())
                    .build();
            postings.add(feeExpenseDebitPosting);
        }

        PostingCommand receivableCreditPosting = PostingCommand.builder()
                .ledgerAccountId(ledgerAccounts.paymentReceivableAccountId())
                .direction(DebitCredit.CREDIT)
                .money(message.grossAmount())
                .build();
        postings.add(receivableCreditPosting);

        return CreateAndPostJournalEntryCommand.builder()
                .reference(reference)
                .postings(postings)
                .build();
    }
}
