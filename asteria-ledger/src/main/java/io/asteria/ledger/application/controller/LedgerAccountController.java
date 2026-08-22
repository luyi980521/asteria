package io.asteria.ledger.application.controller;

import io.asteria.ledger.application.command.CreateLedgerAccountCommand;
import io.asteria.ledger.application.request.CreateLedgerAccountRequest;
import io.asteria.ledger.application.service.LedgerAccountApplicationService;
import io.asteria.ledger.domain.valueobject.LedgerAccountId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Currency;

/**
 * 账本账户功能接口定义
 * */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ledger/accounts")
public class LedgerAccountController {

    private final LedgerAccountApplicationService ledgerAccountApplicationService;

    /**
     * 创建账本账户
     * */
    @PostMapping
    public LedgerAccountId create(@RequestBody CreateLedgerAccountRequest request) {
        log.info("Received create ledger account request, accountCode: {}", request.accountCode());
        CreateLedgerAccountCommand command = new CreateLedgerAccountCommand(
                request.accountCode(),
                request.ownerType(),
                request.ownerId(),
                request.category(),
                Currency.getInstance(request.currency()),
                request.allowNegativeBalance()
        );

        return ledgerAccountApplicationService.createLedgerAccount(command);
    }
}
