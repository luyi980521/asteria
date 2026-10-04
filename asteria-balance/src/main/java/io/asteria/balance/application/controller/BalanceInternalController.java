package io.asteria.balance.application.controller;

import io.asteria.balance.api.request.ConsumeBalanceRequest;
import io.asteria.balance.api.request.ReleaseBalanceRequest;
import io.asteria.balance.api.request.ReserveBalanceRequest;
import io.asteria.balance.application.assembler.BalanceRequestAssembler;
import io.asteria.balance.application.service.BalanceApplicationService;
import io.asteria.web.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/balance")
public class BalanceInternalController {

    private final BalanceApplicationService balanceApplicationService;
    private final BalanceRequestAssembler balanceRequestAssembler;

    @PostMapping("/reserve")
    public ApiResponse<Void> reserve(@RequestBody ReserveBalanceRequest request) {
        balanceApplicationService.reserve(balanceRequestAssembler.toCommand(request));
        return ApiResponse.successWithoutData();
    }

    @PostMapping("/release")
    public ApiResponse<Void> release(@RequestBody ReleaseBalanceRequest request) {
        balanceApplicationService.release(balanceRequestAssembler.toCommand(request));
        return ApiResponse.successWithoutData();
    }

    @PostMapping("/consume")
    public ApiResponse<Void> consume(@RequestBody ConsumeBalanceRequest request) {
        balanceApplicationService.consume(balanceRequestAssembler.toCommand(request));
        return ApiResponse.successWithoutData();
    }
}