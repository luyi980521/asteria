package io.asteria.settlement.application.service.impl;

import io.asteria.settlement.application.assembler.SettlementBatchAssembler;
import io.asteria.settlement.application.command.CreateSettlementBatchCommand;
import io.asteria.settlement.application.service.SettlementBatchApplicationService;
import io.asteria.settlement.domain.entity.SettlementBatch;
import io.asteria.settlement.domain.error.SettlementErrorCode;
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
                    throw new SettlementDomainException(
                            SettlementErrorCode.SETTLEMENT_BATCH_ALREADY_EXISTS);
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


    }
}