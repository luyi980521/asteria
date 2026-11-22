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
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import io.asteria.common.util.JsonUtils;

import static io.asteria.reconciliation.ReconciliationFixtures.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * 在真实 PostgreSQL 上验证 DDL 与八套映射；只创建临时表，结束时回滚并关闭连接。
 */
class ReconciliationPersistenceTest {
    private static final String[] TYPES = {"Rule", "SourceRecord", "Run", "MatchResult", "Case", "CaseAction", "FileBatch", "DeliveryBatch"};
    private static final String[] TABLES = {"reconciliation_rule", "reconciliation_source_record", "reconciliation_run",
            "reconciliation_match_result", "reconciliation_case", "reconciliation_case_action", "reconciliation_file_batch", "reconciliation_delivery_batch"};
    private SqlSession session;
    private ReconciliationRuleRepositoryImpl rules;
    private ReconciliationSourceRecordRepositoryImpl sources;
    private ReconciliationRunRepositoryImpl runs;
    private ReconciliationMatchResultRepositoryImpl results;
    private ReconciliationCaseRepositoryImpl cases;
    private ReconciliationCaseActionRepositoryImpl actions;
    private ReconciliationFileBatchRepositoryImpl files;
    private ReconciliationDeliveryBatchRepositoryImpl deliveries;

    /**
     * 按既有执行顺序加载与 DO 对应的 snake_case 完整建表 SQL。
     *
     * @throws Exception 数据库连接或初始化失败
     */
    @BeforeEach
    void setUp() throws Exception {
        String url = System.getenv("ASTERIA_RECONCILIATION_TEST_JDBC_URL");
        assumeTrue(url != null && !url.isBlank(), "PostgreSQL integration test URL is not configured");
        var dataSource = new UnpooledDataSource("org.postgresql.Driver", url,
                System.getenv("ASTERIA_RECONCILIATION_TEST_DB_USER"),
                System.getenv("ASTERIA_RECONCILIATION_TEST_DB_PASSWORD"));
        var configuration = new MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true);
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
            String resource = "sql/" + table + ".sql";
            try (var input = getClass().getClassLoader().getResourceAsStream(resource);
                 var statement = connection.createStatement()) {
                assertNotNull(input, resource);
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
        files = new ReconciliationFileBatchRepositoryImpl(session.getMapper(ReconciliationFileBatchMapper.class),
                new ReconciliationFileBatchPersistenceConverter());
        deliveries = new ReconciliationDeliveryBatchRepositoryImpl(session.getMapper(ReconciliationDeliveryBatchMapper.class),
                new ReconciliationDeliveryBatchPersistenceConverter());
    }

    @Test
    void sourceBatchInsertsIgnoreOnlySourceKeyAndPreserveOriginalEvidence() throws Exception {
        assertTrue(sources.saveIfAbsent(source(2L, 0L)));
        assertFalse(sources.saveIfAbsent(source(20L, 0L)));
        assertEquals(1, sources.saveAllIfAbsent(java.util.List.of(source(21L, 0L), source(22L, 1L), source(23L, 1L))));
        assertEquals(0, sources.saveAllIfAbsent(java.util.List.of()));
        assertEquals(2L, sources.findBySource(ReconciliationSourceType.PAYMENT, "channel-a:payment-1", 0L)
                .orElseThrow().getRecordId().value());
        assertEquals(22L, sources.findBySource(ReconciliationSourceType.PAYMENT, "channel-a:payment-1", 1L)
                .orElseThrow().getRecordId().value());
        assertConflict(() -> sources.saveIfAbsent(source(2L, 2L)));
    }

    @Test
    void fileRegistrationIsIdempotentAndImportStatusUsesCompareAndSet() throws Exception {
        deliveries.insert(ReconciliationDeliveryBatch.builder().batchId(ReconciliationDeliveryBatchId.of(200L))
                .sourceSystem("PAYMENT").manifestObjectKey("payment/manifest.json").fileVersion(0L).schemaVersion(1)
                .businessDate(LocalDate.of(2026, 10, 1)).windowStart(START).windowEnd(END).cutoffAt(NOW)
                .expectedRecordCount(2L).status(ReconciliationFileBatchStatus.DELIVERED).createdAt(NOW).updatedAt(NOW).build());
        var batch = ReconciliationFileBatch.builder()
                .fileId(ReconciliationFileBatchId.of(100L))
                .objectKey("payment/captures.csv")
                .sourceSystem("PAYMENT")
                .recordType("PAYMENT_CAPTURE")
                .windowStart(START)
                .windowEnd(END)
                .fileVersion(0L)
                .schemaVersion(1)
                .sha256("a".repeat(64))
                .recordCount(2L)
                .byteSize(100L)
                .status(ReconciliationFileBatchStatus.CREATED)
                .failureReason(null)
                .createdAt(NOW)
                .completedAt(null)
                .batchId(ReconciliationDeliveryBatchId.of(200L))
                .businessDate(java.time.LocalDate.of(2026, 10, 1))
                .cutoffAt(NOW)
                .actualRecordCount(null)
                .updatedAt(NOW)
                .build();
        assertTrue(files.saveIfAbsent(batch));
        assertFalse(files.saveIfAbsent(batch));
        var differentDigest = ReconciliationFileBatch.builder()
                .fileId(ReconciliationFileBatchId.of(101L))
                .objectKey(batch.getObjectKey())
                .sourceSystem(batch.getSourceSystem())
                .recordType(batch.getRecordType())
                .windowStart(START)
                .windowEnd(END)
                .fileVersion(0L)
                .schemaVersion(1)
                .sha256("b".repeat(64))
                .recordCount(3L)
                .byteSize(120L)
                .status(ReconciliationFileBatchStatus.CREATED)
                .failureReason(null)
                .createdAt(NOW)
                .completedAt(null)
                .batchId(ReconciliationDeliveryBatchId.of(200L))
                .businessDate(batch.getBusinessDate())
                .cutoffAt(NOW)
                .actualRecordCount(null)
                .updatedAt(NOW)
                .build();
        assertFalse(files.saveIfAbsent(differentDigest));
        assertSnapshot(batch, files.findByDelivery("PAYMENT", batch.getObjectKey(), 0L).orElseThrow());
        assertTrue(files.updateImportStatus(ReconciliationFileBatchId.of(100L), "CREATED", "IMPORTING", null, null));
        assertFalse(files.updateImportStatus(ReconciliationFileBatchId.of(100L), "CREATED", "IMPORTING", null, null));
        assertTrue(files.updateImportStatus(ReconciliationFileBatchId.of(100L), "IMPORTING", "COMPLETED", NOW.plusSeconds(1), null));
        var completed = files.findById(ReconciliationFileBatchId.of(100L)).orElseThrow();
        assertEquals(ReconciliationFileBatchStatus.COMPLETED, completed.getStatus());
        assertEquals(NOW.plusSeconds(1), completed.getCompletedAt());
        assertSqlState("23514", () -> files.updateImportStatus(ReconciliationFileBatchId.of(100L), "COMPLETED", "UNKNOWN", NOW.plusSeconds(2), null));
        assertEquals(1, files.findByBatchId(ReconciliationDeliveryBatchId.of(200L), 0, 10).size());
        assertEquals(1, files.findBySourceAndBusinessDate("PAYMENT", batch.getBusinessDate(), 0, 10).size());
        assertEquals(1, files.findByStatus(ReconciliationFileBatchStatus.COMPLETED, 0, 10).size());
    }

    @Test
    void deliveryHeadersSupportMultiFileIdentityPagingAndConcurrentMetadataUpdates() {
        var day = java.time.LocalDate.of(2026, 10, 1);
        var batch = ReconciliationDeliveryBatch.builder()
                .batchId(ReconciliationDeliveryBatchId.of(200L))
                .manifestObjectKey("ledger/manifest.json")
                .sourceSystem("LEDGER")
                .businessDate(day)
                .windowStart(START)
                .windowEnd(END)
                .cutoffAt(NOW)
                .fileVersion(0L)
                .schemaVersion(1)
                .expectedRecordCount(30L)
                .actualRecordCount(null)
                .status(ReconciliationFileBatchStatus.CREATED)
                .failureReason(null)
                .createdAt(NOW)
                .updatedAt(NOW)
                .completedAt(null)
                .build();
        assertTrue(deliveries.saveIfAbsent(batch));
        assertFalse(deliveries.saveIfAbsent(batch));
        assertSnapshot(batch, deliveries.findById(ReconciliationDeliveryBatchId.of(200L)).orElseThrow());
        assertSnapshot(batch, deliveries.findByDelivery("LEDGER", batch.getManifestObjectKey(), 0L).orElseThrow());
        assertEquals(1, deliveries.findBySourceAndBusinessDate("LEDGER", day, 0, 1).size());
        assertTrue(deliveries.findBySourceAndBusinessDate("LEDGER", day, 200, 1).isEmpty());
        assertEquals(1, deliveries.findByStatus(ReconciliationFileBatchStatus.CREATED, 0, 10).size());
        var updated = ReconciliationDeliveryBatch.builder()
                .batchId(ReconciliationDeliveryBatchId.of(200L))
                .manifestObjectKey(batch.getManifestObjectKey())
                .sourceSystem("LEDGER")
                .businessDate(day)
                .windowStart(START)
                .windowEnd(END)
                .cutoffAt(NOW)
                .fileVersion(0L)
                .schemaVersion(1)
                .expectedRecordCount(30L)
                .actualRecordCount(29L)
                .status(ReconciliationFileBatchStatus.QUARANTINED)
                .failureReason("file evidence")
                .createdAt(NOW)
                .updatedAt(NOW.plusSeconds(1))
                .completedAt(NOW.plusSeconds(1))
                .build();
        assertTrue(deliveries.updateMetadata(updated, ReconciliationFileBatchStatus.CREATED, NOW));
        assertFalse(deliveries.updateMetadata(updated, ReconciliationFileBatchStatus.CREATED, NOW));
        assertSnapshot(updated, deliveries.findById(ReconciliationDeliveryBatchId.of(200L)).orElseThrow());
        assertThrows(IllegalArgumentException.class, () -> deliveries.findByStatus(ReconciliationFileBatchStatus.CREATED, -1, 10));
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

    /**
     * 验证 Rule、Run、Case 的完整更新快照、数据库版本递增及过期版本拒绝行为。
     */
    @Test
    void versionedUpdatesRejectStaleRuleRunAndCaseWithoutOverwritingState() {
        rules.save(rule());
        var firstRule = rules.findById(ReconciliationRuleId.of(1L)).orElseThrow();
        var staleRule = rules.findById(ReconciliationRuleId.of(1L)).orElseThrow();
        firstRule.configure(false, 90L, null, NOW.plusSeconds(1));
        rules.update(firstRule);
        assertVersionedSnapshot(firstRule, rules.findById(firstRule.getRuleId()).orElseThrow());
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
        assertVersionedSnapshot(running, runs.findById(running.getRunId()).orElseThrow());
        assertEquals(ReconciliationRunStatus.COMPLETED, runs.findById(ReconciliationRunId.of(3L)).orElseThrow().getStatus());

        cases.save(reconciliationCase(5L));
        var firstCase = cases.findById(ReconciliationCaseId.of(5L)).orElseThrow();
        var staleCase = cases.findById(ReconciliationCaseId.of(5L)).orElseThrow();
        firstCase.ignore("忽略原因", NOW.plusSeconds(1));
        cases.update(firstCase);
        assertVersionedSnapshot(firstCase, cases.findById(firstCase.getCaseId()).orElseThrow());
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

    /**
     * 按全部增量执行后的真实临时表逐列核对 DO，覆盖字段集合、类型、主键及可空性。
     *
     * @throws Exception 数据库查询或类型读取失败
     */
    @Test
    void finalDdlMatchesEveryDoColumnTypeAndNullability() throws Exception {
        Map<String, Set<String>> nullable = Map.of(
                "Rule", Set.of("details"),
                "Run", Set.of("started_at", "finished_at", "failure_reason"),
                "SourceRecord", Set.of("amount", "currency", "posting_status"),
                "MatchResult", Set.of("difference_type", "expected_amount", "expected_currency", "actual_amount", "actual_currency", "details"),
                "Case", Set.of("assignee", "details", "resolution", "closed_at"),
                "CaseAction", Set.of("from_status", "details"),
                "FileBatch", Set.of("failure_reason", "completed_at", "batch_id", "business_date", "cutoff_at", "actual_record_count"),
                "DeliveryBatch", Set.of("actual_record_count", "failure_reason", "completed_at"));
        for (String type : TYPES) {
            Class<?> doType = Class.forName("io.asteria.reconciliation.infrastructure.persistence.dataobject.Reconciliation" + type + "DO");
            var table = TableInfoHelper.getTableInfo(doType);
            Map<String, Class<?>> properties = new TreeMap<>();
            properties.put(table.getKeyColumn(), doType.getDeclaredField(table.getKeyProperty()).getType());
            for (var field : table.getFieldList()) properties.put(field.getColumn(), doType.getDeclaredField(field.getProperty()).getType());
            List<String> expectedOrder = new ArrayList<>();
            expectedOrder.add(table.getKeyColumn());
            table.getFieldList().forEach(field -> expectedOrder.add(field.getColumn()));
            List<String> actualOrder = new ArrayList<>();
            Set<String> columns = new TreeSet<>();
            Set<String> nullableColumns = new TreeSet<>();
            try (var query = session.getConnection().prepareStatement(
                    "SELECT a.attname, format_type(a.atttypid,a.atttypmod) AS field_type, a.attnotnull "
                    + "FROM pg_attribute a JOIN pg_class c ON c.oid=a.attrelid "
                    + "WHERE c.relnamespace=pg_my_temp_schema() AND c.relname=? AND a.attnum>0 AND NOT a.attisdropped ORDER BY a.attnum")) {
                query.setString(1, table.getTableName());
                try (var rows = query.executeQuery()) {
                    while (rows.next()) {
                        String column = rows.getString("attname");
                        String sqlType = rows.getString("field_type");
                        columns.add(column);
                        actualOrder.add(column);
                        if (!rows.getBoolean("attnotnull")) nullableColumns.add(column);
                        Class<?> javaType = properties.get(column);
                        assertNotNull(javaType, table.getTableName() + "." + column);
                        if (javaType == Long.class) assertEquals("bigint", sqlType);
                        else if (javaType == Integer.class) assertEquals("integer", sqlType);
                        else if (javaType == Instant.class) assertEquals("timestamp with time zone", sqlType);
                        else if (javaType == LocalDate.class) assertEquals("date", sqlType);
                        else if (javaType == boolean.class) assertEquals("boolean", sqlType);
                        else if (javaType == java.math.BigDecimal.class) assertEquals("numeric(38,18)", sqlType);
                        else if (javaType == String.class) assertTrue(sqlType.equals("text") || sqlType.startsWith("character varying("), sqlType);
                        else fail("Unreviewed Java persistence type: " + javaType);
                    }
                }
            }
            assertEquals(properties.keySet(), columns, type + " final DDL column set");
            assertEquals(expectedOrder, actualOrder, type + " SQL and DO declaration order");
            assertEquals(nullable.get(type), nullableColumns, type + " nullable columns");
        }
    }

    /**
     * 用完整领域结构比较八条 Repository 的 INSERT 和 SELECT，而不是只验证部分字段。
     */
    @Test
    void allEightRepositoriesPreserveCompleteSnapshots() {
        var rule = rule();
        rules.save(rule);
        assertSnapshot(rule, rules.findById(rule.getRuleId()).orElseThrow());
        var sourceConverter = new ReconciliationSourceRecordPersistenceConverter();
        var sourceRow = sourceConverter.toDataObject(source(2L, 987L));
        sourceRow.setAmount(new java.math.BigDecimal("99999999999999999999.99"));
        var source = sourceConverter.toDomain(sourceRow);
        sources.save(source);
        assertSnapshot(source, sources.findById(source.getRecordId()).orElseThrow());
        var run = run();
        runs.save(run);
        assertSnapshot(run, runs.findById(run.getRunId()).orElseThrow());
        var result = result(4L, ReconciliationMatchStatus.MISMATCHED, ReconciliationDifferenceType.CURRENCY_MISMATCH);
        results.save(result);
        assertSnapshot(result, results.findById(result.getMatchResultId()).orElseThrow());
        var caseRecord = reconciliationCase(5L);
        cases.save(caseRecord);
        assertSnapshot(caseRecord, cases.findById(caseRecord.getCaseId()).orElseThrow());
        var action = action();
        actions.save(action);
        assertSnapshot(action, actions.findById(action.getCaseActionId()).orElseThrow());
        var delivery = ReconciliationPersistenceContractTest.delivery(ReconciliationFileBatchStatus.CREATED, null, null);
        deliveries.saveIfAbsent(delivery);
        assertSnapshot(delivery, deliveries.findById(delivery.getBatchId()).orElseThrow());
        var file = ReconciliationPersistenceContractTest.file(ReconciliationFileBatchStatus.CREATED, null, null);
        files.saveIfAbsent(file);
        assertSnapshot(file, files.findById(file.getFileId()).orElseThrow());
        assertSnapshot(file, files.findByBatchId(delivery.getBatchId(), 0, 1).getFirst());
    }

    /**
     * 验证文件元数据更新覆盖实际数量与处理时间，能显式清空可空列，并保留不可变描述。
     */
    @Test
    void fileMetadataUpdatesPreserveIdentityAndClearNullableColumns() {
        var original = ReconciliationPersistenceContractTest.file(ReconciliationFileBatchStatus.CREATED, null, null);
        files.saveIfAbsent(original);
        var updated = ReconciliationFileBatch.builder()
                .fileId(original.getFileId())
                .objectKey("ignored.csv")
                .sourceSystem("ignored-source")
                .recordType("ignored-type")
                .windowStart(START)
                .windowEnd(END)
                .fileVersion(999L)
                .schemaVersion(99)
                .sha256("b".repeat(64))
                .recordCount(999L)
                .byteSize(999L)
                .status(ReconciliationFileBatchStatus.FAILED)
                .failureReason("失败\n\"中文\",")
                .createdAt(original.getCreatedAt())
                .completedAt(NOW.plusSeconds(1))
                .batchId(ReconciliationDeliveryBatchId.of(999L))
                .businessDate(LocalDate.of(2000, 1, 1))
                .cutoffAt(NOW.plusSeconds(10))
                .actualRecordCount(13L)
                .updatedAt(NOW.plusSeconds(1))
                .build();
        assertTrue(files.updateMetadata(updated, original.getStatus(), original.getUpdatedAt()));
        var stored = files.findById(original.getFileId()).orElseThrow();
        assertEquals(original.getObjectKey(), stored.getObjectKey());
        assertEquals(original.getSourceSystem(), stored.getSourceSystem());
        assertEquals(original.getFileVersion(), stored.getFileVersion());
        assertEquals(original.getSchemaVersion(), stored.getSchemaVersion());
        assertEquals(original.getSha256(), stored.getSha256());
        assertEquals(original.getRecordCount(), stored.getRecordCount());
        assertEquals(original.getByteSize(), stored.getByteSize());
        assertEquals(original.getBatchId(), stored.getBatchId());
        assertEquals(original.getBusinessDate(), stored.getBusinessDate());
        assertEquals(original.getCutoffAt(), stored.getCutoffAt());
        assertEquals(updated.getActualRecordCount(), stored.getActualRecordCount());
        assertEquals(updated.getFailureReason(), stored.getFailureReason());
        assertEquals(updated.getCompletedAt(), stored.getCompletedAt());
        assertEquals(updated.getUpdatedAt(), stored.getUpdatedAt());
        assertFalse(files.updateMetadata(updated, original.getStatus(), original.getUpdatedAt()));
        var cleared = ReconciliationFileBatch.builder()
                .fileId(stored.getFileId())
                .objectKey(stored.getObjectKey())
                .sourceSystem(stored.getSourceSystem())
                .recordType(stored.getRecordType())
                .windowStart(START)
                .windowEnd(END)
                .fileVersion(stored.getFileVersion())
                .schemaVersion(stored.getSchemaVersion())
                .sha256(stored.getSha256())
                .recordCount(stored.getRecordCount())
                .byteSize(stored.getByteSize())
                .status(stored.getStatus())
                .failureReason(null)
                .createdAt(stored.getCreatedAt())
                .completedAt(null)
                .batchId(stored.getBatchId())
                .businessDate(stored.getBusinessDate())
                .cutoffAt(stored.getCutoffAt())
                .actualRecordCount(null)
                .updatedAt(NOW.plusSeconds(2))
                .build();
        assertTrue(files.updateMetadata(cleared, stored.getStatus(), stored.getUpdatedAt()));
        assertSnapshot(cleared, files.findById(stored.getFileId()).orElseThrow());
    }

    /**
     * 验证跨 500 行分块的批量写入与来源三元组唯一约束一致。
     */
    @Test
    void sourceBatchAcrossChunkBoundaryIsIdempotent() {
        var batch = java.util.stream.LongStream.range(0, 501).mapToObj(index -> source(1000L + index, index)).toList();
        assertEquals(501, sources.saveAllIfAbsent(batch));
        assertEquals(0, sources.saveAllIfAbsent(batch));
        assertSnapshot(batch.getLast(), sources.findById(batch.getLast().getRecordId()).orElseThrow());
    }

    /**
     * 比较完整领域快照，涵盖 TEXT 的原始内容及精确数值。
     *
     * @param expected 写入前快照
     * @param actual 读取后快照
     */
    private static void assertSnapshot(Object expected, Object actual) {
        assertEquals(JsonUtils.mapper().valueToTree(expected), JsonUtils.mapper().valueToTree(actual));
    }

    /**
     * 数据库条件更新递增版本，其余字段必须与调用方提交的完整快照一致。
     *
     * @param expected 更新时的领域快照
     * @param actual 数据库恢复的快照
     */
    private static void assertVersionedSnapshot(Object expected, Object actual) {
        var expectedJson = (com.fasterxml.jackson.databind.node.ObjectNode) JsonUtils.mapper().valueToTree(expected);
        expectedJson.put("version", expectedJson.path("version").longValue() + 1);
        assertEquals(expectedJson, JsonUtils.mapper().valueToTree(actual));
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
