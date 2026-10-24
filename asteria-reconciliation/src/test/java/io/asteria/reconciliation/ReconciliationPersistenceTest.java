package io.asteria.reconciliation;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisSqlSessionFactoryBuilder;
import io.asteria.reconciliation.domain.entity.*;
import io.asteria.reconciliation.domain.enums.*;
import io.asteria.reconciliation.domain.error.ReconciliationErrorCode;
import io.asteria.reconciliation.domain.exception.ReconciliationDomainException;
import io.asteria.reconciliation.domain.valueobject.*;
import io.asteria.reconciliation.infrastructure.persistence.converter.*;
import io.asteria.reconciliation.infrastructure.persistence.mapper.*;
import io.asteria.reconciliation.infrastructure.persistence.repository.*;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.LocalCacheScope;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.sql.SQLException;

import static io.asteria.reconciliation.ReconciliationFixtures.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * 在真实 PostgreSQL 上验证 DDL 与六套映射；只创建临时表，结束时回滚并关闭连接。
 */
class ReconciliationPersistenceTest {
    private static final String[] TYPES = {"Rule", "SourceRecord", "Run", "MatchResult", "Case", "CaseAction"};
    private static final String[] TABLES = {"reconciliation_rule", "reconciliation_source_record", "reconciliation_run",
            "reconciliation_match_result", "reconciliation_case", "reconciliation_case_action"};
    private SqlSession session;
    private ReconciliationRuleRepositoryImpl rules;
    private ReconciliationSourceRecordRepositoryImpl sources;
    private ReconciliationRunRepositoryImpl runs;
    private ReconciliationMatchResultRepositoryImpl results;
    private ReconciliationCaseRepositoryImpl cases;
    private ReconciliationCaseActionRepositoryImpl actions;

    @BeforeEach
    void setUp() throws Exception {
        String url = System.getenv("ASTERIA_RECONCILIATION_TEST_JDBC_URL");
        assumeTrue(url != null && !url.isBlank(), "PostgreSQL integration test URL is not configured");
        var dataSource = new UnpooledDataSource("org.postgresql.Driver", url,
                System.getenv("ASTERIA_RECONCILIATION_TEST_DB_USER"),
                System.getenv("ASTERIA_RECONCILIATION_TEST_DB_PASSWORD"));
        var configuration = new MybatisConfiguration();
        configuration.setEnvironment(new Environment("reconciliation-test", new JdbcTransactionFactory(), dataSource));
        configuration.setLocalCacheScope(LocalCacheScope.STATEMENT);
        for (String type : TYPES) {
            String resource = "mapper/Reconciliation" + type + "Mapper.xml";
            try (var input = getClass().getClassLoader().getResourceAsStream(resource)) {
                assertNotNull(input, resource);
                new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
            }
        }
        var connection = dataSource.getConnection();
        connection.setAutoCommit(false);
        session = new MybatisSqlSessionFactoryBuilder().build(configuration).openSession(connection);
        for (String table : TABLES) {
            try (var input = getClass().getClassLoader().getResourceAsStream("sql/" + table + ".sql");
                 var statement = connection.createStatement()) {
                assertNotNull(input, table);
                // 仅改变表的生命周期，其余 DDL、索引和注释完全使用交付脚本。
                statement.execute(new String(input.readAllBytes(), StandardCharsets.UTF_8)
                        .replace("CREATE TABLE ", "CREATE TEMPORARY TABLE "));
            }
        }
        rules = new ReconciliationRuleRepositoryImpl(session.getMapper(ReconciliationRuleMapper.class),
                new ReconciliationRulePersistenceConverter());
        sources = new ReconciliationSourceRecordRepositoryImpl(session.getMapper(ReconciliationSourceRecordMapper.class),
                new ReconciliationSourceRecordPersistenceConverter());
        runs = new ReconciliationRunRepositoryImpl(session.getMapper(ReconciliationRunMapper.class),
                new ReconciliationRunPersistenceConverter());
        results = new ReconciliationMatchResultRepositoryImpl(session.getMapper(ReconciliationMatchResultMapper.class),
                new ReconciliationMatchResultPersistenceConverter());
        cases = new ReconciliationCaseRepositoryImpl(session.getMapper(ReconciliationCaseMapper.class),
                new ReconciliationCasePersistenceConverter());
        actions = new ReconciliationCaseActionRepositoryImpl(session.getMapper(ReconciliationCaseActionMapper.class),
                new ReconciliationCaseActionPersistenceConverter());
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
    void allSixRepositoriesRoundTripAndUseBusinessKeyLookups() {
        assertTrue(rules.findById(ReconciliationRuleId.of(1L)).isEmpty());
        rules.save(rule());
        sources.save(source(2L, 0L));
        runs.save(run());
        results.save(result(4L, ReconciliationMatchStatus.MISSING, ReconciliationDifferenceType.MISSING_LEDGER));
        cases.save(reconciliationCase(5L));
        actions.save(action());
        assertEquals("PAYMENT_LEDGER_V1", rules.findById(ReconciliationRuleId.of(1L)).orElseThrow().getRuleCode());
        assertEquals(DETAILS, rules.findByRuleCode("PAYMENT_LEDGER_V1").orElseThrow().getDetails());
        assertEquals(money("0", "USD"), sources.findById(ReconciliationSourceRecordId.of(2L)).orElseThrow().getAmount());
        assertEquals(0L, sources.findBySource(ReconciliationSourceType.PAYMENT, "channel-a:payment-1", 0L)
                .orElseThrow().getSourceVersion());
        assertEquals(START, runs.findById(ReconciliationRunId.of(3L)).orElseThrow().getWindowStart());
        assertEquals("[2]", results.findById(ReconciliationMatchResultId.of(4L)).orElseThrow().getSourceRecordIds());
        assertEquals(money("10", "JPY"), results.findByRunAndBusinessKey(ReconciliationRunId.of(3L),
                "PAYMENT_LEDGER_V1", "merchant-1:payment-1").orElseThrow().getActualAmount());
        assertEquals(ReconciliationCaseId.of(5L), cases.findActive("PAYMENT_LEDGER_V1", "merchant-1:payment-1",
                ReconciliationDifferenceType.MISSING_LEDGER).orElseThrow().getCaseId());
        assertEquals(ReconciliationCaseStatus.OPEN, cases.findById(ReconciliationCaseId.of(5L)).orElseThrow().getStatus());
        assertEquals("reconciliation", actions.findById(ReconciliationCaseActionId.of(6L)).orElseThrow().getOperatorId());
    }

    @Test
    void sourceTripleRejectsReplayButRetainsDifferentVersionsAndSameBusinessSnapshots() throws Exception {
        sources.save(source(2L, 0L));
        assertConflict(() -> sources.save(source(7L, 0L)));
        sources.save(source(7L, 1L));
        assertEquals(ReconciliationSourceRecordId.of(7L), sources.findBySource(ReconciliationSourceType.PAYMENT,
                "channel-a:payment-1", 1L).orElseThrow().getRecordId());
    }

    @Test
    void matchRunRuleBusinessKeyIsUniqueAndRerunHasIndependentHistory() throws Exception {
        results.save(result(4L, ReconciliationMatchStatus.MISSING, ReconciliationDifferenceType.MISSING_LEDGER));
        assertConflict(() -> results.save(result(7L, ReconciliationMatchStatus.MISSING, ReconciliationDifferenceType.MISSING_LEDGER)));
        // 不建立物理外键，数据库允许先保存关联方；一致性由未来应用事务负责。
        execute("INSERT INTO reconciliation_match_result (id, run_id, rule_code, business_key, status, source_record_ids, created_at) "
                + "VALUES (8, 9, 'PAYMENT_LEDGER_V1', 'merchant-1:payment-1', 'PENDING', '[2]', CURRENT_TIMESTAMP)");
        assertEquals(ReconciliationMatchStatus.PENDING, results.findById(ReconciliationMatchResultId.of(8L)).orElseThrow().getStatus());
    }

    @Test
    void activeCaseUniqueIndexCoversOpenAndInvestigatingAndReleasesOnBothTerminalStates() throws Exception {
        cases.save(reconciliationCase(5L));
        assertConflict(() -> cases.save(reconciliationCase(7L)));
        var c = cases.findById(ReconciliationCaseId.of(5L)).orElseThrow();
        c.investigate("operator-1", NOW.plusSeconds(1));
        cases.update(c);
        assertConflict(() -> cases.save(reconciliationCase(7L)));
        c = cases.findById(ReconciliationCaseId.of(5L)).orElseThrow();
        c.resolve("已核实入账", NOW.plusSeconds(2));
        cases.update(c);
        cases.save(reconciliationCase(7L));
        var second = cases.findById(ReconciliationCaseId.of(7L)).orElseThrow();
        second.ignore("重复导入", NOW.plusSeconds(3));
        cases.update(second);
        cases.save(reconciliationCase(8L));
        assertEquals(8L, cases.findActive("PAYMENT_LEDGER_V1", "merchant-1:payment-1",
                ReconciliationDifferenceType.MISSING_LEDGER).orElseThrow().getCaseId().value());
    }

    @Test
    void versionedUpdatesRejectStaleRuleRunAndCaseWithoutOverwritingState() {
        rules.save(rule());
        var firstRule = rules.findById(ReconciliationRuleId.of(1L)).orElseThrow();
        var staleRule = rules.findById(ReconciliationRuleId.of(1L)).orElseThrow();
        firstRule.configure(false, 90L, DETAILS, NOW.plusSeconds(1));
        rules.update(firstRule);
        staleRule.configure(true, 120L, "{}", NOW.plusSeconds(2));
        assertConcurrent(() -> rules.update(staleRule));
        assertEquals(1L, rules.findByRuleCode("PAYMENT_LEDGER_V1").orElseThrow().getVersion());
        assertFalse(rules.findByRuleCode("PAYMENT_LEDGER_V1").orElseThrow().isEnabled());

        runs.save(run());
        var firstRun = runs.findById(ReconciliationRunId.of(3L)).orElseThrow();
        var staleRun = runs.findById(ReconciliationRunId.of(3L)).orElseThrow();
        firstRun.start(NOW.plusSeconds(1));
        runs.update(firstRun);
        staleRun.start(NOW.plusSeconds(2));
        assertConcurrent(() -> runs.update(staleRun));
        var running = runs.findById(ReconciliationRunId.of(3L)).orElseThrow();
        running.complete(1L, 0L, 0L, 1L, 0L, 0L, NOW.plusSeconds(3));
        runs.update(running);
        assertEquals(ReconciliationRunStatus.COMPLETED, runs.findById(ReconciliationRunId.of(3L)).orElseThrow().getStatus());

        cases.save(reconciliationCase(5L));
        var firstCase = cases.findById(ReconciliationCaseId.of(5L)).orElseThrow();
        var staleCase = cases.findById(ReconciliationCaseId.of(5L)).orElseThrow();
        firstCase.ignore("忽略原因", NOW.plusSeconds(1));
        cases.update(firstCase);
        staleCase.resolve("过期操作", NOW.plusSeconds(2));
        assertConcurrent(() -> cases.update(staleCase));
        assertEquals(ReconciliationCaseStatus.IGNORED, cases.findById(ReconciliationCaseId.of(5L)).orElseThrow().getStatus());
    }

    @Test
    void databaseRejectsInvalidStatusAmountWindowCountsAndAuditTransition() throws Exception {
        rules.save(rule());
        sources.save(source(2L, 0L));
        runs.save(run());
        results.save(result(4L, ReconciliationMatchStatus.MISSING, ReconciliationDifferenceType.MISSING_LEDGER));
        cases.save(reconciliationCase(5L));
        actions.save(action());
        for (String sql : new String[]{
                "UPDATE reconciliation_rule SET left_source_type = 'BANK'",
                "UPDATE reconciliation_source_record SET source_version = -1",
                "UPDATE reconciliation_source_record SET amount = -1",
                "UPDATE reconciliation_source_record SET currency = NULL",
                "UPDATE reconciliation_run SET window_end = window_start",
                "UPDATE reconciliation_run SET matched_count = 1",
                "UPDATE reconciliation_run SET status = 'COMPLETED'",
                "UPDATE reconciliation_run SET status = 'FAILED', started_at = created_at, finished_at = created_at",
                "UPDATE reconciliation_match_result SET difference_type = NULL",
                "UPDATE reconciliation_case SET status = 'INVESTIGATING'",
                "UPDATE reconciliation_case SET status = 'RESOLVED'",
                "UPDATE reconciliation_case_action SET action_type = 'RESOLVE', from_status = NULL, to_status = 'RESOLVED'"}) {
            assertSqlState("23514", () -> execute(sql));
        }
    }

    @Test
    void ddlProvidesEveryCommentTextSerializationAndNoPhysicalForeignKeys() throws Exception {
        var connection = session.getConnection();
        for (String table : TABLES) {
            try (var query = connection.prepareStatement(
                    "SELECT obj_description(c.oid, 'pg_class') AS table_comment, a.attname, "
                    + "col_description(c.oid, a.attnum) AS column_comment "
                    + "FROM pg_class c JOIN pg_attribute a ON a.attrelid = c.oid "
                    + "WHERE c.relnamespace = pg_my_temp_schema() AND c.relname = ? AND a.attnum > 0 AND NOT a.attisdropped")) {
                query.setString(1, table);
                try (var rows = query.executeQuery()) {
                    int count = 0;
                    while (rows.next()) {
                        assertNotNull(rows.getString("table_comment"), table);
                        assertNotNull(rows.getString("column_comment"), table + "." + rows.getString("attname"));
                        assertFalse(rows.getString("column_comment").isBlank());
                        count++;
                    }
                    assertTrue(count > 0, table);
                }
            }
        }
        try (var statement = connection.createStatement();
             var rows = statement.executeQuery("SELECT count(*) FROM pg_constraint p JOIN pg_class c ON c.oid = p.conrelid "
                     + "WHERE c.relnamespace = pg_my_temp_schema() AND p.contype = 'f'")) {
            rows.next();
            assertEquals(0, rows.getInt(1));
        }
        try (var statement = connection.createStatement();
             var rows = statement.executeQuery("SELECT a.attname, format_type(a.atttypid, a.atttypmod) AS field_type "
                     + "FROM pg_class c JOIN pg_attribute a ON a.attrelid = c.oid WHERE c.relnamespace = pg_my_temp_schema() "
                     + "AND a.attnum > 0 AND NOT a.attisdropped")) {
            while (rows.next()) {
                String name = rows.getString("attname");
                String type = rows.getString("field_type");
                assertNotEquals("jsonb", type);
                if (name.equals("payload") || name.equals("details") || name.equals("source_record_ids") || name.equals("rule_snapshot")) {
                    assertEquals("text", type, name);
                }
            }
        }
    }

    private void execute(String sql) throws SQLException {
        try (var statement = session.getConnection().createStatement()) {
            statement.execute(sql);
        }
    }

    private void assertConflict(CheckedAction action) throws Exception {
        assertSqlState("23505", action);
    }

    private void assertSqlState(String expected, CheckedAction action) throws Exception {
        var connection = session.getConnection();
        var savepoint = connection.setSavepoint();
        try {
            Exception failure = assertThrows(Exception.class, action::run);
            Throwable cause = failure;
            while (!(cause instanceof SQLException) && cause.getCause() != null) { cause = cause.getCause(); }
            assertEquals(expected, assertInstanceOf(SQLException.class, cause).getSQLState());
        } finally {
            connection.rollback(savepoint);
            connection.releaseSavepoint(savepoint);
            session.clearCache();
        }
    }

    private static void assertConcurrent(Runnable update) {
        assertEquals(ReconciliationErrorCode.CONCURRENT_MODIFICATION,
                assertThrows(ReconciliationDomainException.class, update::run).errorCode());
    }

    @FunctionalInterface
    private interface CheckedAction {
        void run() throws Exception;
    }
}
