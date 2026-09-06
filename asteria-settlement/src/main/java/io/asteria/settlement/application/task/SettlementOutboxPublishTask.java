package io.asteria.settlement.application.task;

import io.asteria.settlement.application.publisher.SettlementOutboxPublisher;
import io.asteria.common.trace.TraceConstants;
import io.asteria.common.trace.TraceIdGenerator;
import org.slf4j.MDC;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Settlement Outbox 发布定时任务
 * */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "asteria.settlement.outbox",
        name = "publish-task-enabled",
        havingValue = "true"
)
public class SettlementOutboxPublishTask {

    private final SettlementOutboxPublisher settlementOutboxPublisher;

    /**
     * 扫描并发布待发送的 Outbox 事件
     * */
    @Scheduled(fixedDelayString = "${asteria.settlement.outbox.publish-interval-ms:1000}")
    public void publishPendingEvents() {
        try {
            MDC.put(TraceConstants.TRACE_ID, TraceIdGenerator.generate());
            settlementOutboxPublisher.publishPendingEvents();
        } catch (Exception ex) {
            log.error("Failed to execute settlement outbox publish task: {}", ex.getMessage(), ex);
        } finally {
            MDC.remove(TraceConstants.TRACE_ID);
        }
    }
}
