package io.asteria.settlement.application.service.impl;

import io.asteria.common.util.JsonUtils;
import io.asteria.settlement.application.assembler.SettlementBatchAssembler;
import io.asteria.settlement.application.command.CreateSettlementBatchCommand;
import io.asteria.settlement.application.port.channel.SettlementBatchRequest;
import io.asteria.settlement.application.port.channel.SettlementBatchResult;
import io.asteria.settlement.application.port.channel.SettlementChannel;
import io.asteria.settlement.application.port.router.SettlementChannelRouter;
import io.asteria.settlement.application.service.SettlementBatchApplicationService;
import io.asteria.settlement.application.service.SettlementBatchTransactionService;
import io.asteria.settlement.domain.entity.SettlementBatch;
import io.asteria.settlement.domain.error.SettlementErrorCode;
import io.asteria.settlement.domain.exception.SettlementChannelUnknownException;
import io.asteria.settlement.domain.exception.SettlementDomainException;
import io.asteria.settlement.domain.repository.SettlementBatchRepository;
import io.asteria.settlement.domain.valueobject.SettlementBatchId;
import io.asteria.settlement.domain.valueobject.SettlementBatchReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SettlementBatchApplicationServiceImpl implements SettlementBatchApplicationService {

    private final SettlementBatchRepository settlementBatchRepository;
    private final SettlementBatchAssembler settlementBatchAssembler;
    private final SettlementChannelRouter settlementChannelRouter;
    private final SettlementBatchTransactionService settlementBatchTransactionService;

    /**
     * 创建结算批次。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SettlementBatch create(CreateSettlementBatchCommand command) {

        SettlementBatchReference reference = SettlementBatchReference.builder()
                .value(command.reference())
                .build();
        settlementBatchRepository.findByReference(reference)
                .ifPresent(existing -> {
                    log.warn("The settlement batch already exists: {}", JsonUtils.toJson(reference));
                    throw new SettlementDomainException(SettlementErrorCode.SETTLEMENT_BATCH_ALREADY_EXISTS);
                });

        SettlementBatch settlementBatch = settlementBatchAssembler.toSettlementBatch(command);
        settlementBatchRepository.insert(settlementBatch);

        log.info("Settlement batch created, settlementBatchId: {}, reference: {}, itemCount: {}",
                settlementBatch.getSettlementBatchId().value(),
                settlementBatch.getReference().value(),
                settlementBatch.getItems().size());

        return settlementBatch;
    }

    /**
     * 提交结算批次。
     */
    @Override
    public void submit(SettlementBatchId settlementBatchId) {

        SettlementBatch settlementBatch = settlementBatchRepository.findById(settlementBatchId)
                .orElseThrow(() -> {
                    log.warn("The settlement batch doesn't exist: {}", settlementBatchId.value());
                    return new SettlementDomainException(SettlementErrorCode.SETTLEMENT_BATCH_NOT_FOUND);
                });
        SettlementBatchRequest settlementBatchRequest = settlementBatchAssembler.toSettlementBatchRequest(settlementBatch);
        // 先检查是否有支持的结算渠道再推进结算状态，否则可能会一直卡在processing却没有被处理
        SettlementChannel channel = settlementChannelRouter.route(settlementBatch);

        settlementBatchTransactionService.startProcessing(settlementBatchId);

        try {
            SettlementBatchResult batchResult = channel.submit(settlementBatchRequest);
            switch (batchResult.status()) {
                case ACCEPTED -> settlementBatchTransactionService.accept(settlementBatchId, batchResult);
                case REJECTED -> settlementBatchTransactionService.fail(settlementBatchId, batchResult);
                case UNKNOWN -> settlementBatchTransactionService.markSubmitUnknown(settlementBatchId);
            }
        } catch (SettlementChannelUnknownException ex) {
            // 未知结果不更新状态
            log.warn("Settlement batch submit result unknown, settlementBatchId: {}, {}, {}",
                    settlementBatchId.value(), ex.getErrorCode(), ex.getMessage());
            settlementBatchTransactionService.markSubmitUnknown(settlementBatchId);
        } catch (Exception ex) {
            // 预期之外的异常，直接报警
            log.error("Failed to submit settlement batch, settlementBatchId: {}, {}",
                    settlementBatchId.value(), ex.getMessage(), ex);
            throw ex;
        }
    }
}