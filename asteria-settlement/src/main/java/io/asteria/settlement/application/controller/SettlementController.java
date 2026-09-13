package io.asteria.settlement.application.controller;

import io.asteria.common.util.JsonUtils;
import io.asteria.settlement.application.command.CompleteSettlementBatchCommand;
import io.asteria.settlement.application.command.CreateSettlementBatchCommand;
import io.asteria.settlement.application.service.SettlementBatchApplicationService;
import io.asteria.settlement.domain.valueobject.SettlementBatchId;
import io.asteria.web.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 结算功能入口
 * */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/settlements/batches")
public class SettlementController {

    private final SettlementBatchApplicationService settlementBatchApplicationService;

    @PostMapping
    public ApiResponse<SettlementBatchId> create(@RequestBody CreateSettlementBatchCommand command) {
        log.info("Received settlement create request: {}", JsonUtils.toJson(command));
        return ApiResponse.success(settlementBatchApplicationService.create(command));
    }

    @PostMapping("/{id}/submit")
    public ApiResponse<Void> submit(@PathVariable("id") Long id) {
        log.info("Received settlement submit request: {}", id);
        SettlementBatchId settlementBatchId = SettlementBatchId.of(id);
        settlementBatchApplicationService.submit(settlementBatchId);
        return ApiResponse.successWithoutData();
    }

    @PostMapping("/complete")
    public ApiResponse<Void> complete(@RequestBody CompleteSettlementBatchCommand command) {
        log.info("Received settlement complete request: {}", JsonUtils.toJson(command));
        settlementBatchApplicationService.complete(command);
        return ApiResponse.successWithoutData();
    }
}
