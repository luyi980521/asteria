package io.asteria.payment;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisSqlSessionFactoryBuilder;
import io.asteria.common.domain.valueobject.*;
import io.asteria.common.util.JsonUtils;
import io.asteria.payment.domain.enums.*;
import io.asteria.payment.domain.event.PaymentCapturedEvent;
import io.asteria.payment.domain.valueobject.PaymentOutboxEvent;
import io.asteria.payment.infrastructure.persistence.converter.PaymentOutboxEventPersistenceConverter;
import io.asteria.payment.infrastructure.persistence.repository.PaymentOutboxEventRepositoryImpl;
import io.asteria.payment.infrastructure.persistence.mapper.PaymentOutboxEventMapper;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * 真实 PostgreSQL 临时表验证捕获事实窗口；不向现有业务表写入。
 */
class PaymentCaptureExportQueryTest {
    @Test
    void queriesEventTimeWithStableIdPagingAndKeepsMultipleCaptures() throws Exception {
        String url = System.getenv("ASTERIA_RECONCILIATION_TEST_JDBC_URL");
        assumeTrue(url != null && !url.isBlank());
        var dataSource = new UnpooledDataSource("org.postgresql.Driver", url,
                System.getenv("ASTERIA_RECONCILIATION_TEST_DB_USER"), System.getenv("ASTERIA_RECONCILIATION_TEST_DB_PASSWORD"));
        var config = new MybatisConfiguration();
        config.setMapUnderscoreToCamelCase(true);
        config.setEnvironment(new Environment("capture-export-test", new JdbcTransactionFactory(), dataSource));
        String resource = "mapper/PaymentOutboxEventMapper.xml";
        try (var input = getClass().getClassLoader().getResourceAsStream(resource)) {
            new XMLMapperBuilder(input, config, resource, config.getSqlFragments()).parse();
        }
        try (var connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try (var session = new MybatisSqlSessionFactoryBuilder().build(config).openSession(connection)) {
                try (var input = getClass().getClassLoader().getResourceAsStream("sql/payment_outbox_event.sql");
                     var statement = connection.createStatement()) {
                    statement.execute(new String(input.readAllBytes(), StandardCharsets.UTF_8).replace("CREATE TABLE ", "CREATE TEMPORARY TABLE "));
                }
                var repository = new PaymentOutboxEventRepositoryImpl(session.getMapper(PaymentOutboxEventMapper.class),
                        new PaymentOutboxEventPersistenceConverter());
                Instant start = Instant.parse("2026-11-14T00:00:00Z"), end = start.plusSeconds(60);
                for (int id = 1; id <= 4; id++) {
                    Instant capturedAt = id == 1 ? start.minusSeconds(1) : id == 4 ? end : start.plusSeconds(id - 2);
                    var event = PaymentCapturedEvent.builder().eventId("capture-" + id).paymentId(42L)
                            .paymentReference(new PaymentCapturedEvent.Reference("ORDER", "order-42"))
                            .amount(Money.of(new BigDecimal("10.00"), CurrencyCode.of("USD"))).capturedAt(capturedAt).build();
                    String payload = JsonUtils.toJson(event);
                    // 同时验证 JsonUtils 的真实数字时间和外部文件常用的 ISO 字符串。
                    if (id == 3) {
                        var tree = (com.fasterxml.jackson.databind.node.ObjectNode) JsonUtils.readTree(payload);
                        tree.put("capturedAt", capturedAt.toString());
                        payload = tree.toString();
                    }
                    repository.insert(PaymentOutboxEvent.builder().id((long) id).eventId(event.eventId()).aggregateType("PAYMENT")
                            .aggregateId("42").eventType(PaymentOutboxEventType.PAYMENT_CAPTURED).payload(payload)
                            .status(id == 2 ? PaymentOutboxEventStatus.PENDING : PaymentOutboxEventStatus.PUBLISHED)
                            .createdAt(end.plusSeconds(3600)).build());
                }
                assertEquals(2L, repository.findCapturedInWindow(start, end, 0, 1).getFirst().getId());
                assertEquals(3L, repository.findCapturedInWindow(start, end, 2, 1).getFirst().getId());
                assertTrue(repository.findCapturedInWindow(start, end, 3, 1).isEmpty());
                assertEquals(2, repository.findCapturedInWindow(start, end, 0, 100).size());
                assertThrows(IllegalArgumentException.class, () -> repository.findCapturedInWindow(end, start, 0, 100));
                assertThrows(IllegalArgumentException.class, () -> repository.findCapturedInWindow(start, end, -1, 100));
                session.rollback(true);
            }
        }
    }
}
