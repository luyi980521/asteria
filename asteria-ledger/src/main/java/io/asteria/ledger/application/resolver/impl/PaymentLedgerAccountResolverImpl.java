package io.asteria.ledger.application.resolver.impl;

import io.asteria.ledger.application.model.PaymentCapturedLedgerAccounts;
import io.asteria.ledger.application.resolver.PaymentLedgerAccountResolver;
import io.asteria.ledger.domain.entity.LedgerAccount;
import io.asteria.ledger.domain.error.LedgerErrorCode;
import io.asteria.ledger.domain.exception.LedgerDomainException;
import io.asteria.ledger.domain.repository.LedgerAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Payment 记账账户解析器实现类
 * */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentLedgerAccountResolverImpl implements PaymentLedgerAccountResolver {

    private final LedgerAccountRepository ledgerAccountRepository;

    /**
     * 解析支付捕获成功对应的账本账户
     */
    @Override
    public PaymentCapturedLedgerAccounts resolve(String currency) {

        String receivableAccountCode = "PAYMENT_RECEIVABLE_" + currency;
        String settlementPayableAccountCode = "SETTLEMENT_PAYABLE_" + currency;

        LedgerAccount receivableAccount = ledgerAccountRepository.findByAccountCode(receivableAccountCode);
        if (receivableAccount == null) {
            log.error("The receivable account doesn't exist: {}", receivableAccountCode);
            throw new LedgerDomainException(LedgerErrorCode.LEDGER_ACCOUNT_NOT_EXIST);
        }

        LedgerAccount settlementPayableAccount = ledgerAccountRepository.findByAccountCode(settlementPayableAccountCode);
        if (settlementPayableAccount == null) {
            log.error("The settlement payable account doesn't exist: {}", settlementPayableAccountCode);
            throw new LedgerDomainException(LedgerErrorCode.LEDGER_ACCOUNT_NOT_EXIST);
        }

        return PaymentCapturedLedgerAccounts.builder()
                .debitAccountId(receivableAccount.getLedgerAccountId())
                .creditAccountId(settlementPayableAccount.getLedgerAccountId())
                .build();
    }
}