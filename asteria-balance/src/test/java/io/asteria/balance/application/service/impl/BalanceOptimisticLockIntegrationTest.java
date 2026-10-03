package io.asteria.balance.application.service.impl;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import io.asteria.balance.application.command.ReserveBalanceCommand;
import io.asteria.balance.application.service.BalanceApplicationService;
import io.asteria.balance.domain.entity.Balance;
import io.asteria.balance.domain.enums.BalanceMovementType;
import io.asteria.balance.domain.enums.BalanceReservationStatus;
import io.asteria.balance.domain.error.BalanceErrorCode;
import io.asteria.balance.domain.exception.BalanceDomainException;
import io.asteria.balance.domain.repository.BalanceRepository;
import io.asteria.balance.domain.repository.BalanceReservationRepository;
import io.asteria.balance.domain.valueobject.BalanceAccountId;
import io.asteria.balance.domain.valueobject.BalanceId;
import io.asteria.balance.infrastructure.persistence.converter.BalanceMovementPersistenceConverter;
import io.asteria.balance.infrastructure.persistence.converter.BalancePersistenceConverter;
import io.asteria.balance.infrastructure.persistence.converter.BalanceReservationPersistenceConverter;
import io.asteria.balance.infrastructure.persistence.dataobject.BalanceDO;
import io.asteria.balance.infrastructure.persistence.dataobject.BalanceReservationDO;
import io.asteria.balance.infrastructure.persistence.mapper.BalanceMapper;
import io.asteria.balance.infrastructure.persistence.repository.BalanceMovementRepositoryImpl;
import io.asteria.balance.infrastructure.persistence.repository.BalanceRepositoryImpl;
import io.asteria.balance.infrastructure.persistence.repository.BalanceReservationRepositoryImpl;
import io.asteria.common.application.port.DistributedIdGenerator;
import io.asteria.common.domain.valueobject.CurrencyCode;
import io.asteria.common.domain.valueobject.Money;
import io.asteria.infrastructure.id.IdGeneratorProperties;
import io.asteria.infrastructure.id.SnowflakeIdGenerator;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.JdbcTemplateAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.transaction.autoconfigure.TransactionAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 使用与 BalanceRepositoryIntegrationTest 相同的三个 ASTERIA_BALANCE_TEST_* 环境变量。
 * 每次运行建立独立 schema，多连接共享测试数据，结束后删除该 schema。
 */
@SpringBootTest(classes = BalanceOptimisticLockIntegrationTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "mybatis-plus.mapper-locations=classpath*:mapper/**/*.xml",
                "mybatis-plus.configuration.map-underscore-to-camel-case=true",
                "mybatis-plus.global-config.db-config.id-type=input",
                "spring.datasource.hikari.maximum-pool-size=4",
                "spring.datasource.hikari.transaction-isolation=TRANSACTION_READ_COMMITTED",
                "spring.datasource.hikari.connection-init-sql=SET statement_timeout = '15s'",
                "asteria.id-generator.worker-id=31",
                "asteria.id-generator.datacenter-id=31"
        })
@EnabledIfEnvironmentVariable(named = "ASTERIA_BALANCE_TEST_JDBC_URL", matches = ".+")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class BalanceOptimisticLockIntegrationTest {
    private static final String SCHEMA = "balance_lock_test_" + UUID.randomUUID().toString().replace("-", "");
    private static final CurrencyCode USD = CurrencyCode.of("USD");
    private static final String REFERENCE_TYPE = "TEST";
    private static final String BALANCE_QUERY =
            "io.asteria.balance.infrastructure.persistence.mapper.BalanceMapper.findByBalanceAccountIdAndCurrency";
    private static final String BALANCE_UPDATE =
            "io.asteria.balance.infrastructure.persistence.mapper.BalanceMapper.updateWithVersion";
    private static final String RESERVATION_INSERT =
            "io.asteria.balance.infrastructure.persistence.mapper.BalanceReservationMapper.insert";

    @Autowired
    private BalanceApplicationService service;
    @Autowired
    private BalanceRepository balanceRepository;
    @Autowired
    private BalanceReservationRepository reservationRepository;
    @Autowired
    private DistributedIdGenerator idGenerator;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private ConcurrentReserveInterceptor race;

    private BalanceId balanceId;
    private BalanceAccountId accountId;
    private boolean schemaCreated;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        String url = System.getenv("ASTERIA_BALANCE_TEST_JDBC_URL");
        registry.add("spring.datasource.url",
                () -> url + (url.contains("?") ? "&" : "?") + "currentSchema=" + SCHEMA);
        registry.add("spring.datasource.username", () -> System.getenv("ASTERIA_BALANCE_TEST_DB_USER"));
        registry.add("spring.datasource.password", () -> System.getenv("ASTERIA_BALANCE_TEST_DB_PASSWORD"));
    }

    @BeforeAll
    void prepareDatabase() {
        jdbc.execute("CREATE SCHEMA " + SCHEMA);
        schemaCreated = true;
        ResourceDatabasePopulator scripts = new ResourceDatabasePopulator();
        scripts.setSqlScriptEncoding("UTF-8");
        for (String table : new String[]{"balance", "balance_reservation", "balance_movement"}) {
            scripts.addScript(new ClassPathResource("sql/" + table + ".sql"));
        }
        scripts.execute(jdbc.getDataSource());
        balanceId = BalanceId.of(idGenerator.nextId());
        accountId = BalanceAccountId.of(idGenerator.nextId());
        Instant now = Instant.now();
        balanceRepository.save(Balance.builder().balanceId(balanceId).balanceAccountId(accountId).currency(USD)
                .availableAmount(money("100")).reservedAmount(money("0")).version(0L)
                .createdAt(now).updatedAt(now).build());
    }

    @AfterAll
    void cleanDatabase() {
        if (schemaCreated) {
            jdbc.execute("DROP SCHEMA " + SCHEMA + " CASCADE");
        }
    }

    @Test
    void concurrent_reserve_should_reject_stale_version_and_roll_back_failed_request() throws Exception {
        ReserveBalanceCommand commandA = command("TEST_RESERVE_A");
        ReserveBalanceCommand commandB = command("TEST_RESERVE_B");
        assertNotEquals(commandA.getEventId(), commandB.getEventId());
        Balance initial = balanceRepository.findByBalanceId(balanceId).orElseThrow();
        assertEquals(money("100"), initial.getAvailableAmount());
        assertEquals(money("0"), initial.getReservedAmount());
        assertEquals(0L, initial.getVersion());

        CountDownLatch start = new CountDownLatch(1);
        var threads = Executors.newFixedThreadPool(2);
        List<ReserveOutcome> outcomes;
        race.enabled = true;
        try {
            var requestA = threads.submit(() -> reserveAfterStart(commandA, start));
            var requestB = threads.submit(() -> reserveAfterStart(commandB, start));
            start.countDown();
            outcomes = List.of(requestA.get(30, TimeUnit.SECONDS), requestB.get(30, TimeUnit.SECONDS));
        } finally {
            start.countDown();
            threads.shutdownNow();
            try {
                assertTrue(threads.awaitTermination(20, TimeUnit.SECONDS), "Reserve workers must finish before cleanup");
            } finally {
                race.enabled = false;
            }
        }

        assertEquals(List.of(0L, 0L), new ArrayList<>(race.readVersions.values()));
        assertEquals(List.of(0L, 0L), new ArrayList<>(race.updateVersions.values()));
        assertEquals(2, race.backendIds.values().stream().distinct().count(),
                "Each request must use a separate PostgreSQL connection");
        assertEquals(List.of(0, 1), race.affectedRows.stream().sorted().toList());
        // 两个 INSERT 都已成功执行；失败请求的预留记录消失只能依靠事务回滚。
        assertEquals(2, race.insertedReservations.size());
        assertTrue(race.insertedReservations.containsKey(commandA.getReferenceId()));
        assertTrue(race.insertedReservations.containsKey(commandB.getReferenceId()));

        List<ReserveOutcome> successes = outcomes.stream().filter(outcome -> outcome.failure() == null).toList();
        List<ReserveOutcome> failures = outcomes.stream().filter(outcome -> outcome.failure() != null).toList();
        assertEquals(1, successes.size());
        assertEquals(1, failures.size());
        assertEquals(BalanceErrorCode.BALANCE_CONCURRENT_MODIFICATION, failures.getFirst().failure().errorCode());

        Balance persisted = balanceRepository.findByBalanceId(balanceId).orElseThrow();
        assertEquals(money("20"), persisted.getAvailableAmount());
        assertEquals(money("80"), persisted.getReservedAmount());
        assertEquals(1L, persisted.getVersion());
        assertEquals(1L, count("SELECT COUNT(*) FROM balance_reservation WHERE balance_id = ?", balanceId.value()));
        assertEquals(1L, count("SELECT COUNT(*) FROM balance_movement WHERE balance_id = ? AND movement_type = ?",
                balanceId.value(), BalanceMovementType.RESERVE.name()));

        ReserveBalanceCommand winner = successes.getFirst().command();
        ReserveBalanceCommand loser = failures.getFirst().command();
        var reservation = reservationRepository.findByBalanceIdAndReference(
                balanceId, REFERENCE_TYPE, winner.getReferenceId()).orElseThrow();
        assertEquals(BalanceReservationStatus.RESERVED, reservation.getStatus());
        assertEquals(money("80"), reservation.getAmount());
        assertEquals(race.insertedReservations.get(winner.getReferenceId()), reservation.getReservationId().value());
        assertTrue(reservationRepository.findByBalanceIdAndReference(
                balanceId, REFERENCE_TYPE, loser.getReferenceId()).isEmpty());

        assertEquals(1L, count("SELECT COUNT(*) FROM balance_movement"
                        + " WHERE balance_id = ? AND movement_type = ? AND reference_type = ?"
                        + " AND reference_id = ? AND event_id = ? AND amount = 80 AND currency = 'USD'",
                balanceId.value(), BalanceMovementType.RESERVE.name(), REFERENCE_TYPE,
                winner.getReferenceId(), winner.getEventId()));
        assertEquals(0L, count("SELECT COUNT(*) FROM balance_reservation WHERE id = ?",
                race.insertedReservations.get(loser.getReferenceId())));
        assertEquals(0L, count("SELECT COUNT(*) FROM balance_movement"
                        + " WHERE balance_id = ? AND (reference_id = ? OR event_id = ?)",
                balanceId.value(), loser.getReferenceId(), loser.getEventId()));
    }

    private ReserveOutcome reserveAfterStart(ReserveBalanceCommand command, CountDownLatch start) throws Exception {
        if (!start.await(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Reserve start timed out");
        }
        try {
            // 调用 Spring 事务代理；返回或抛异常时，该线程的事务已提交或回滚。
            service.reserve(command);
            return new ReserveOutcome(command, null);
        } catch (BalanceDomainException exception) {
            return new ReserveOutcome(command, exception);
        }
    }

    private ReserveBalanceCommand command(String referenceId) {
        return ReserveBalanceCommand.builder().balanceAccountId(accountId).currency(USD).amount(money("80"))
                .referenceType(REFERENCE_TYPE).referenceId(referenceId)
                .eventId(idGenerator.nextId().toString()).build();
    }

    private long count(String sql, Object... parameters) {
        return jdbc.queryForObject(sql, Long.class, parameters);
    }

    private static Money money(String amount) {
        return Money.ofNonNegative(new BigDecimal(amount), USD);
    }

    private record ReserveOutcome(ReserveBalanceCommand command, BalanceDomainException failure) {
    }

    @SpringBootConfiguration
    @ImportAutoConfiguration({
            DataSourceAutoConfiguration.class, DataSourceTransactionManagerAutoConfiguration.class,
            JdbcTemplateAutoConfiguration.class, TransactionAutoConfiguration.class, MybatisPlusAutoConfiguration.class
    })
    @EnableConfigurationProperties(IdGeneratorProperties.class)
    @MapperScan(basePackageClasses = BalanceMapper.class)
    @Import({
            BalanceApplicationServiceImpl.class,
            BalanceRepositoryImpl.class, BalanceReservationRepositoryImpl.class, BalanceMovementRepositoryImpl.class,
            BalancePersistenceConverter.class, BalanceReservationPersistenceConverter.class,
            BalanceMovementPersistenceConverter.class, SnowflakeIdGenerator.class
    })
    static class TestApplication {
        @Bean
        ConcurrentReserveInterceptor concurrentReserveInterceptor() {
            return new ConcurrentReserveInterceptor();
        }
    }

    /** 只同步真实 Mapper 的调用，不替换 Repository、不改变 SQL 或影响行数。 */
    @Intercepts({
            @Signature(type = Executor.class, method = "query",
                    args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
            @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class})
    })
    static class ConcurrentReserveInterceptor implements Interceptor {
        private final CyclicBarrier reads = new CyclicBarrier(2);
        private final CyclicBarrier updates = new CyclicBarrier(2);
        private final Map<Long, Long> readVersions = new ConcurrentHashMap<>();
        private final Map<Long, Long> updateVersions = new ConcurrentHashMap<>();
        private final Map<Long, Integer> backendIds = new ConcurrentHashMap<>();
        private final Map<String, Long> insertedReservations = new ConcurrentHashMap<>();
        private final ConcurrentLinkedQueue<Integer> affectedRows = new ConcurrentLinkedQueue<>();
        private volatile boolean enabled;

        @Override
        public Object intercept(Invocation invocation) throws Throwable {
            if (!enabled) {
                return invocation.proceed();
            }
            String statement = ((MappedStatement) invocation.getArgs()[0]).getId();
            long threadId = Thread.currentThread().threadId();
            if (BALANCE_QUERY.equals(statement)) {
                Object result = invocation.proceed();
                BalanceDO row = (BalanceDO) ((List<?>) result).getFirst();
                assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
                readVersions.put(threadId, row.getVersion());
                var connection = ((Executor) invocation.getTarget()).getTransaction().getConnection();
                try (var query = connection.createStatement(); var backend = query.executeQuery("SELECT pg_backend_pid()")) {
                    assertTrue(backend.next());
                    backendIds.put(threadId, backend.getInt(1));
                }
                // 两次查询均完成后才让应用层继续，保证两个领域对象都来自 version 0。
                reads.await(10, TimeUnit.SECONDS);
                return result;
            }
            if (RESERVATION_INSERT.equals(statement)) {
                Object result = invocation.proceed();
                assertEquals(1, result);
                BalanceReservationDO reservation = (BalanceReservationDO) invocation.getArgs()[1];
                insertedReservations.put(reservation.getReferenceId(), reservation.getId());
                return result;
            }
            if (BALANCE_UPDATE.equals(statement)) {
                BalanceDO balance = (BalanceDO) invocation.getArgs()[1];
                assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
                updateVersions.put(threadId, balance.getVersion());
                // 此时两个事务已各自插入预留记录，随后同时竞争余额行。
                updates.await(10, TimeUnit.SECONDS);
                Object result = invocation.proceed();
                affectedRows.add((Integer) result);
                return result;
            }
            return invocation.proceed();
        }
    }
}
