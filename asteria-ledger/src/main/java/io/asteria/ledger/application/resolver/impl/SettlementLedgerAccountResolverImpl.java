package io.asteria.ledger.application.resolver.impl;

import io.asteria.ledger.application.model.SettlementCompletedLedgerAccounts;
import io.asteria.ledger.application.resolver.SettlementLedgerAccountResolver;
import io.asteria.ledger.domain.entity.LedgerAccount;
import io.asteria.ledger.domain.repository.LedgerAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Settlement 记账账户解析器实现类
 * */
@Slf4j
@Component
@RequiredArgsConstructor
public class SettlementLedgerAccountResolverImpl implements SettlementLedgerAccountResolver {

    private final LedgerAccountRepository ledgerAccountRepository;

    /**
     * 解析结算完成对应的账本账户
     */
    @Override
    public SettlementCompletedLedgerAccounts resolve(String currency) {

        LedgerAccount cashAccount = ledgerAccountRepository.findByAccountCode("BANK_CASH_" + currency);
        LedgerAccount feeExpenseAccount = ledgerAccountRepository.findByAccountCode("PROCESSING_FEE_EXPENSE_" + currency);
        LedgerAccount receivableAccount = ledgerAccountRepository.findByAccountCode("PAYMENT_RECEIVABLE_" + currency);
        return SettlementCompletedLedgerAccounts.builder()
                .cashAccountId(cashAccount.getLedgerAccountId())
                .processingFeeExpenseAccountId(feeExpenseAccount.getLedgerAccountId())
                .paymentReceivableAccountId(receivableAccount.getLedgerAccountId())
                .build();
    }
}
