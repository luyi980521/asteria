package io.asteria.balance.infrastructure.persistence;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisSqlSessionFactoryBuilder;
import io.asteria.balance.domain.entity.Balance;
import io.asteria.balance.domain.entity.BalanceMovement;
import io.asteria.balance.domain.entity.BalanceReservation;
import io.asteria.balance.domain.enums.BalanceMovementType;
import io.asteria.balance.domain.enums.BalanceReservationStatus;
import io.asteria.balance.domain.error.BalanceErrorCode;
import io.asteria.balance.domain.exception.BalanceDomainException;
import io.asteria.balance.domain.repository.BalanceMovementRepository;
import io.asteria.balance.domain.repository.BalanceRepository;
import io.asteria.balance.domain.repository.BalanceReservationRepository;
import io.asteria.balance.domain.valueobject.BalanceAccountId;
import io.asteria.balance.domain.valueobject.BalanceId;
import io.asteria.balance.domain.valueobject.BalanceMovementId;
import io.asteria.balance.domain.valueobject.BalanceReservationId;
import io.asteria.balance.infrastructure.persistence.converter.BalanceMovementPersistenceConverter;
import io.asteria.balance.infrastructure.persistence.converter.BalancePersistenceConverter;
import io.asteria.balance.infrastructure.persistence.converter.BalanceReservationPersistenceConverter;
import io.asteria.balance.infrastructure.persistence.mapper.BalanceMovementMapper;
import io.asteria.balance.infrastructure.persistence.mapper.BalanceMapper;
import io.asteria.balance.infrastructure.persistence.mapper.BalanceReservationMapper;
import io.asteria.balance.infrastructure.persistence.repository.BalanceMovementRepositoryImpl;
import io.asteria.balance.infrastructure.persistence.repository.BalanceRepositoryImpl;
import io.asteria.balance.infrastructure.persistence.repository.BalanceReservationRepositoryImpl;
import io.asteria.common.domain.valueobject.CurrencyCode;
import io.asteria.common.domain.valueobject.Money;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.exceptions.PersistenceException;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.LocalCacheScope;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** 设置 ASTERIA_BALANCE_TEST_JDBC_URL、ASTERIA_BALANCE_TEST_DB_USER、ASTERIA_BALANCE_TEST_DB_PASSWORD 后运行。 */
class BalanceRepositoryIntegrationTest {
    private static final CurrencyCode USD = CurrencyCode.of("USD");
    private static final BalanceId BALANCE_ID = BalanceId.of(1L);
    private static final Instant CREATED = Instant.parse("2026-01-01T00:00:00Z");
    private SqlSession session;
    private BalanceRepository balances;
    private BalanceReservationRepository reservations;
    private BalanceMovementRepository movements;

    @BeforeEach
    void setUp() throws Exception {
        String url = System.getenv("ASTERIA_BALANCE_TEST_JDBC_URL");
        assumeTrue(url != null && !url.isBlank(), "PostgreSQL integration test URL is not configured");
        MybatisConfiguration configuration = new MybatisConfiguration();
        var dataSource = new UnpooledDataSource("org.postgresql.Driver", url,
                System.getenv("ASTERIA_BALANCE_TEST_DB_USER"), System.getenv("ASTERIA_BALANCE_TEST_DB_PASSWORD"));
        configuration.setEnvironment(new Environment("balance-test", new JdbcTransactionFactory(), dataSource));
        configuration.setLocalCacheScope(LocalCacheScope.STATEMENT);
        for (String mapper : new String[]{"BalanceMapper", "BalanceReservationMapper", "BalanceMovementMapper"}) {
            String resource = "mapper/" + mapper + ".xml";
            try (var input = getClass().getClassLoader().getResourceAsStream(resource)) {
                assertNotNull(input, resource);
                new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
            }
        }
        var connection = dataSource.getConnection();
        connection.setAutoCommit(false);
        session = new MybatisSqlSessionFactoryBuilder().build(configuration).openSession(connection);
        // 临时表仅对当前连接可见；关闭连接并回滚，不改动已有业务表。
        for (String table : new String[]{"balance", "balance_reservation", "balance_movement"}) {
            try (var input = getClass().getClassLoader().getResourceAsStream("sql/" + table + ".sql");
                 var statement = connection.createStatement()) {
                assertNotNull(input, table);
                String sql = new String(input.readAllBytes(), StandardCharsets.UTF_8)
                        .replace("CREATE TABLE ", "CREATE TEMPORARY TABLE ");
                statement.execute(sql);
            }
        }
        balances = new BalanceRepositoryImpl(session.getMapper(BalanceMapper.class), new BalancePersistenceConverter());
        reservations = new BalanceReservationRepositoryImpl(session.getMapper(BalanceReservationMapper.class),
                new BalanceReservationPersistenceConverter());
        movements = new BalanceMovementRepositoryImpl(session.getMapper(BalanceMovementMapper.class),
                new BalanceMovementPersistenceConverter());
    }

    @AfterEach
    void tearDown() {
        if (session != null) {
            try {
                session.rollback(true);
            } finally {
                session.close();
            }
        }
    }

    @Test
    void balanceUpdateAdvancesDatabaseVersionAndRejectsStaleSnapshot() {
        assertTrue(balances.findByBalanceId(BALANCE_ID).isEmpty());
        assertTrue(balances.findByBalanceAccountIdAndCurrency(BalanceAccountId.of(2L), USD).isEmpty());
        balances.save(balance());
        Balance first = balances.findByBalanceId(BALANCE_ID).orElseThrow();
        Balance stale = balances.findByBalanceAccountIdAndCurrency(BalanceAccountId.of(2L), USD).orElseThrow();
        assertEquals(7L, first.getVersion());
        assertEquals(7L, stale.getVersion());
        first.reserve(Money.of(BigDecimal.ONE, USD));
        balances.update(first);
        assertEquals(7L, first.getVersion());
        Balance restored = balances.findByBalanceId(BALANCE_ID).orElseThrow();
        assertEquals(8L, restored.getVersion());
        assertEquals(Money.of(new BigDecimal("9"), USD), restored.getAvailableAmount());
        assertEquals(Money.of(BigDecimal.ONE, USD), restored.getReservedAmount());
        assertEquals(CREATED, restored.getCreatedAt());
        assertTrue(Duration.between(first.getUpdatedAt(), restored.getUpdatedAt()).abs().toNanos() < 1000);

        stale.credit(Money.of(BigDecimal.ONE, USD));
        assertEquals(BalanceErrorCode.BALANCE_CONCURRENT_MODIFICATION,
                assertThrows(BalanceDomainException.class, () -> balances.update(stale)).errorCode());
        Balance afterConflict = balances.findByBalanceId(BALANCE_ID).orElseThrow();
        assertEquals(restored.getAvailableAmount(), afterConflict.getAvailableAmount());
        assertEquals(restored.getReservedAmount(), afterConflict.getReservedAmount());
        assertEquals(8L, afterConflict.getVersion());

        restored.credit(Money.of(BigDecimal.ONE, USD));
        balances.update(restored);
        assertEquals(9L, balances.findByBalanceId(BALANCE_ID).orElseThrow().getVersion());
    }

    @Test
    void balanceUpdateOfMissingRowAlsoReportsConditionalUpdateConflict() {
        assertEquals(BalanceErrorCode.BALANCE_CONCURRENT_MODIFICATION,
                assertThrows(BalanceDomainException.class, () -> balances.update(balance())).errorCode());
    }

    @Test
    void reservationSaveFindAndUpdateUseReservedStatus() {
        assertTrue(reservations.findByReservationId(BalanceReservationId.of(3L)).isEmpty());
        assertTrue(reservations.findByBalanceIdAndReference(BALANCE_ID, "PAYMENT", "payment-3").isEmpty());
        for (long id : new long[]{3L, 4L}) {
            BalanceReservation reservation = BalanceReservation.builder().reservationId(BalanceReservationId.of(id))
                    .balanceId(BALANCE_ID).amount(Money.of(BigDecimal.ONE, USD))
                    .status(BalanceReservationStatus.RESERVED).referenceType("PAYMENT")
                    .referenceId("payment-" + id).createdAt(CREATED).updatedAt(CREATED).build();
            reservations.save(reservation);
            BalanceReservation restored = reservations.findByReservationId(reservation.getReservationId()).orElseThrow();
            assertEquals(BalanceReservationStatus.RESERVED, restored.getStatus());
            assertEquals(reservation.getAmount(), restored.getAmount());
            if (id == 3L) {
                restored.release();
            } else {
                restored.consume();
            }
            reservations.update(restored);
            BalanceReservation updated = reservations.findByBalanceIdAndReference(
                    BALANCE_ID, "PAYMENT", "payment-" + id).orElseThrow();
            assertEquals(restored.getStatus(), updated.getStatus());
            assertEquals(CREATED, updated.getCreatedAt());
            assertTrue(Duration.between(restored.getUpdatedAt(), updated.getUpdatedAt()).abs().toNanos() < 1000);
        }
    }

    @Test
    void movementExistenceUsesEventBalanceAndTypeAndDatabaseEnforcesUniqueness() {
        assertFalse(movements.existsByEventIdAndBalanceIdAndMovementType("event-1", BALANCE_ID,
                BalanceMovementType.CREDIT));
        movements.save(movement(5L));
        assertTrue(movements.existsByEventIdAndBalanceIdAndMovementType("event-1", BALANCE_ID,
                BalanceMovementType.CREDIT));
        assertFalse(movements.existsByEventIdAndBalanceIdAndMovementType("event-2", BALANCE_ID,
                BalanceMovementType.CREDIT));
        assertFalse(movements.existsByEventIdAndBalanceIdAndMovementType("event-1", BalanceId.of(2L),
                BalanceMovementType.CREDIT));
        assertFalse(movements.existsByEventIdAndBalanceIdAndMovementType("event-1", BALANCE_ID,
                BalanceMovementType.RESERVE));
        assertThrows(PersistenceException.class, () -> movements.save(movement(6L)));
    }

    private static Balance balance() {
        return Balance.builder().balanceId(BALANCE_ID).balanceAccountId(BalanceAccountId.of(2L)).currency(USD)
                .availableAmount(Money.of(new BigDecimal("10"), USD))
                .reservedAmount(Money.ofNonNegative(BigDecimal.ZERO, USD))
                .version(7L).createdAt(CREATED).updatedAt(CREATED).build();
    }

    private static BalanceMovement movement(long id) {
        return BalanceMovement.builder().movementId(BalanceMovementId.of(id)).balanceId(BALANCE_ID)
                .movementType(BalanceMovementType.CREDIT).amount(Money.of(BigDecimal.ONE, USD))
                .referenceType("PAYMENT").referenceId("payment-1").eventId("event-1").createdAt(CREATED).build();
    }
}
