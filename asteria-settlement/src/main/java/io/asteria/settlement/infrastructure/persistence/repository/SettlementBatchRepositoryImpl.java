package io.asteria.settlement.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.asteria.settlement.domain.entity.SettlementBatch;
import io.asteria.settlement.domain.repository.SettlementBatchRepository;
import io.asteria.settlement.domain.valueobject.SettlementBatchId;
import io.asteria.settlement.domain.valueobject.SettlementBatchReference;
import io.asteria.settlement.infrastructure.persistence.converter.SettlementPersistenceConverter;
import io.asteria.settlement.infrastructure.persistence.dataobject.SettlementBatchDO;
import io.asteria.settlement.infrastructure.persistence.dataobject.SettlementItemDO;
import io.asteria.settlement.infrastructure.persistence.mapper.SettlementBatchMapper;
import io.asteria.settlement.infrastructure.persistence.mapper.SettlementItemMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Settlement Batch 仓储实现。
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class SettlementBatchRepositoryImpl implements SettlementBatchRepository {

    private final SettlementBatchMapper settlementBatchMapper;
    private final SettlementItemMapper settlementItemMapper;
    private final SettlementPersistenceConverter converter;

    /**
     * 新增结算批次。
     */
    @Override
    public void insert(SettlementBatch settlementBatch) {
        SettlementBatchDO batchDO = converter.toBatchDO(settlementBatch);
        settlementBatchMapper.insert(batchDO);

        List<SettlementItemDO> itemDOList = settlementBatch.getItems().stream()
                .map(item -> converter.toItemDO(settlementBatch.getSettlementBatchId(), item))
                .toList();

        settlementItemMapper.batchInsert(itemDOList);

        log.info("Settlement batch inserted, settlementBatchId: {}, itemCount: {}",
                settlementBatch.getSettlementBatchId().value(),
                itemDOList.size());
    }

    /**
     * 更新结算批次。
     */
    @Override
    public void update(SettlementBatch settlementBatch) {
        SettlementBatchDO batchDO = converter.toBatchDO(settlementBatch);
        settlementBatchMapper.updateById(batchDO);

        log.info("Settlement batch updated, settlementBatchId: {}, status: {}",
                settlementBatch.getSettlementBatchId().value(),
                settlementBatch.getStatus());
    }

    /**
     * 根据 ID 查询结算批次。
     */
    @Override
    public Optional<SettlementBatch> findById(SettlementBatchId settlementBatchId) {
        SettlementBatchDO batchDO = settlementBatchMapper.selectById(settlementBatchId.value());

        if (batchDO == null) {
            return Optional.empty();
        }

        List<SettlementItemDO> itemDOList = settlementItemMapper.selectList(
                new LambdaQueryWrapper<SettlementItemDO>()
                        .eq(SettlementItemDO::getSettlementBatchId, settlementBatchId.value())
        );

        return Optional.of(converter.toDomain(batchDO, itemDOList));
    }

    /**
     * 根据业务引用查询结算批次。
     */
    @Override
    public Optional<SettlementBatch> findByReference(SettlementBatchReference reference) {
        SettlementBatchDO batchDO = settlementBatchMapper.selectOne(
                new LambdaQueryWrapper<SettlementBatchDO>()
                        .eq(SettlementBatchDO::getSettlementReference, reference.value())
        );

        if (batchDO == null) {
            return Optional.empty();
        }

        List<SettlementItemDO> itemDOList = settlementItemMapper.selectList(
                new LambdaQueryWrapper<SettlementItemDO>()
                        .eq(SettlementItemDO::getSettlementBatchId, batchDO.getId())
        );

        return Optional.of(converter.toDomain(batchDO, itemDOList));
    }
}