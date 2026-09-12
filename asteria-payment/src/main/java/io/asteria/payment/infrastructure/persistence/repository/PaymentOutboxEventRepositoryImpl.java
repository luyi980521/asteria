package io.asteria.payment.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import io.asteria.payment.domain.enums.PaymentOutboxEventStatus;
import io.asteria.payment.domain.repository.PaymentOutboxEventRepository;
import io.asteria.payment.domain.valueobject.PaymentOutboxEvent;
import io.asteria.payment.infrastructure.persistence.converter.PaymentOutboxEventPersistenceConverter;
import io.asteria.payment.infrastructure.persistence.dataobject.PaymentOutboxEventDO;
import io.asteria.payment.infrastructure.persistence.mapper.PaymentOutboxEventMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * 支付 Outbox 事件仓储实现
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class PaymentOutboxEventRepositoryImpl implements PaymentOutboxEventRepository {

    private final PaymentOutboxEventMapper paymentOutboxEventMapper;
    private final PaymentOutboxEventPersistenceConverter converter;

    @Override
    public List<PaymentOutboxEvent> findByAggregateIds(List<String> aggregateIds) {
        if (aggregateIds == null || aggregateIds.isEmpty()) {
            return List.of();
        }
        return paymentOutboxEventMapper.selectList(
                        new LambdaQueryWrapper<PaymentOutboxEventDO>()
                                .in(PaymentOutboxEventDO::getAggregateId, aggregateIds)
                                .orderByAsc(PaymentOutboxEventDO::getId))
                .stream()
                .map(converter::toDomain)
                .toList();
    }

    /**
     * 插入 Outbox 事件
     */
    @Override
    public void insert(PaymentOutboxEvent event) {
        PaymentOutboxEventDO eventDO = converter.toDO(event);
        paymentOutboxEventMapper.insert(eventDO);

        log.info("Outbox event inserted, eventId={}, eventType={}",
                event.getEventId(), event.getEventType());
    }

    /**
     * 查询待发布事件
     */
    @Override
    public List<PaymentOutboxEvent> findPending(int limit) {
        List<PaymentOutboxEventDO> eventDOList = paymentOutboxEventMapper.selectList(
                new LambdaQueryWrapper<PaymentOutboxEventDO>()
                        .eq(PaymentOutboxEventDO::getStatus, PaymentOutboxEventStatus.PENDING.name())
                        .orderByAsc(PaymentOutboxEventDO::getCreatedAt)
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
        int updated = paymentOutboxEventMapper.update(
                null,
                new LambdaUpdateWrapper<PaymentOutboxEventDO>()
                        .eq(PaymentOutboxEventDO::getId, id)
                        .eq(PaymentOutboxEventDO::getStatus, PaymentOutboxEventStatus.PENDING.name())
                        .set(PaymentOutboxEventDO::getStatus, PaymentOutboxEventStatus.PUBLISHED.name())
                        .set(PaymentOutboxEventDO::getPublishedAt, publishedAt)
        );

        if (updated > 0) {
            log.info("Outbox event marked as published, id={}", id);
        }
    }
}
