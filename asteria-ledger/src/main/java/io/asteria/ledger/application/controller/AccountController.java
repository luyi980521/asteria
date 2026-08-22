package io.asteria.ledger.application.controller;

import io.asteria.ledger.application.command.CreateAccountCommand;
import io.asteria.ledger.application.request.CreateAccountRequest;
import io.asteria.ledger.application.service.AccountApplicationService;
import io.asteria.ledger.domain.valueobject.AccountId;
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
public class AccountController {

    private final AccountApplicationService accountApplicationService;

    /**
     * 创建账本账户
     * */
    @PostMapping
    public AccountId create(@RequestBody CreateAccountRequest request) {
        log.info("Received create ledger account request, accountCode: {}", request.accountCode());
        CreateAccountCommand command = new CreateAccountCommand(
                request.accountCode(),
                request.ownerType(),
                request.ownerId(),
                request.category(),
                Currency.getInstance(request.currency()),
                request.allowNegativeBalance()
        );

        return accountApplicationService.create(command);
    }
}