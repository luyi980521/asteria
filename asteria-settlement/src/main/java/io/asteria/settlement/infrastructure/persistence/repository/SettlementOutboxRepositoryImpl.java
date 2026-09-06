package io.asteria.settlement.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import io.asteria.settlement.domain.enums.SettlementOutboxEventStatus;
import io.asteria.settlement.domain.repository.SettlementOutboxRepository;
import io.asteria.settlement.domain.valueobject.SettlementOutboxEvent;
import io.asteria.settlement.infrastructure.persistence.converter.SettlementOutboxEventPersistenceConverter;
import io.asteria.settlement.infrastructure.persistence.dataobject.SettlementOutboxEventDO;
import io.asteria.settlement.infrastructure.persistence.mapper.SettlementOutboxEventMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * 结算 Outbox 事件仓储实现
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class SettlementOutboxRepositoryImpl implements SettlementOutboxRepository {

    private final SettlementOutboxEventMapper settlementOutboxEventMapper;
    private final SettlementOutboxEventPersistenceConverter converter;

    /**
     * 插入 Outbox 事件
     */
    @Override
    public void insert(SettlementOutboxEvent event) {
        SettlementOutboxEventDO eventDO = converter.toDO(event);
        settlementOutboxEventMapper.insert(eventDO);

        log.info("Outbox event inserted, eventId: {}, settlementBatchId: {}, eventType: {}",
                event.getEventId(), event.getAggregateId(), event.getEventType());
    }

    /**
     * 查询待发布事件
     */
    @Override
    public List<SettlementOutboxEvent> findPending(int limit) {
        List<SettlementOutboxEventDO> eventDOList = settlementOutboxEventMapper.selectList(
                new LambdaQueryWrapper<SettlementOutboxEventDO>()
                        .eq(SettlementOutboxEventDO::getStatus, SettlementOutboxEventStatus.PENDING.name())
                        .orderByAsc(SettlementOutboxEventDO::getCreatedAt)
                        .last("LIMIT " + limit)
        );

        return eventDOList.stream()
                .map(converter::toDomain)
                .toList();
    }

    /**
     * 标记为已发布
     */
    @Override
    public void markPublished(Long id, Instant publishedAt) {
        int updated = settlementOutboxEventMapper.update(
                null,
                new LambdaUpdateWrapper<SettlementOutboxEventDO>()
                        .eq(SettlementOutboxEventDO::getId, id)
                        .eq(SettlementOutboxEventDO::getStatus, SettlementOutboxEventStatus.PENDING.name())
                        .set(SettlementOutboxEventDO::getStatus, SettlementOutboxEventStatus.PUBLISHED.name())
                        .set(SettlementOutboxEventDO::getPublishedAt, publishedAt)
        );

        if (updated > 0) {
            log.info("Outbox event marked as published, id: {}", id);
        }
    }
}