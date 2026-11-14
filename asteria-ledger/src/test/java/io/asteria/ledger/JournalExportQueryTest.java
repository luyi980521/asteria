package io.asteria.ledger;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisSqlSessionFactoryBuilder;
import io.asteria.ledger.infrastructure.persistence.converter.JournalEntryPersistenceConverter;
import io.asteria.ledger.infrastructure.persistence.mapper.*;
import io.asteria.ledger.infrastructure.persistence.repository.JournalEntryRepositoryImpl;
import io.asteria.ledger.domain.valueobject.JournalEntryId;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * 仅使用临时表验证凭证分页、版本保留及批量分录查询。
 */
class JournalExportQueryTest {
    @Test
    void queriesPostedWindowWithVersionAndOrderedPostingBatches() throws Exception {
        String url = System.getenv("ASTERIA_RECONCILIATION_TEST_JDBC_URL");
        assumeTrue(url != null && !url.isBlank());
        var source = new UnpooledDataSource("org.postgresql.Driver", url,
                System.getenv("ASTERIA_RECONCILIATION_TEST_DB_USER"), System.getenv("ASTERIA_RECONCILIATION_TEST_DB_PASSWORD"));
        var config = new MybatisConfiguration();
        config.setMapUnderscoreToCamelCase(true);
        config.setEnvironment(new Environment("journal-export-test", new JdbcTransactionFactory(), source));
        for (String mapper : List.of("JournalEntry", "Posting")) {
            String resource = "mapper/" + mapper + "Mapper.xml";
            try (var input = getClass().getClassLoader().getResourceAsStream(resource)) {
                new XMLMapperBuilder(input, config, resource, config.getSqlFragments()).parse();
            }
        }
        try (var connection = source.getConnection()) {
            connection.setAutoCommit(false);
            try (var session = new MybatisSqlSessionFactoryBuilder().build(config).openSession(connection)) {
                for (String table : List.of("ledger_journal_entry", "ledger_posting")) {
                    try (var input = getClass().getClassLoader().getResourceAsStream("sql/" + table + ".sql");
                         var statement = connection.createStatement()) {
                        // 去掉历史建表脚本的 DROP，只改变新建表生命周期，不触碰正式表。
                        statement.execute(new String(input.readAllBytes(), StandardCharsets.UTF_8)
                                .replaceAll("(?m)^DROP TABLE[^;]*;", "").replace("CREATE TABLE ", "CREATE TEMPORARY TABLE "));
                    }
                }
                try (var statement = connection.createStatement()) {
                    statement.execute("INSERT INTO ledger_journal_entry (id,source_type,source_id,event_type,event_id,status,posted_at,version) VALUES "
                            + "(1,'PAYMENT','pay_42','PAYMENT_CAPTURED','c1','POSTED','2026-11-14T00:00:00Z',3),"
                            + "(2,'PAYMENT','pay_42','PAYMENT_CAPTURED','c2','POSTED','2026-11-14T00:00:59Z',0),"
                            + "(3,'PAYMENT','pay_43','PAYMENT_CAPTURED','c3','POSTED','2026-11-14T00:01:00Z',0),"
                            + "(4,'PAYMENT','pay_44','PAYMENT_CAPTURED','c4','DRAFT',NULL,0)");
                    statement.execute("INSERT INTO ledger_posting (id,journal_entry_id,ledger_account_id,amount,currency,direction,sequence_no) VALUES "
                            + "(12,1,100,10,'USD','CREDIT',2),(11,1,101,10,'USD','DEBIT',1),"
                            + "(22,2,100,20,'USD','CREDIT',9),(21,2,101,20,'USD','DEBIT',5)");
                }
                var repository = new JournalEntryRepositoryImpl(session.getMapper(JournalEntryMapper.class),
                        session.getMapper(PostingMapper.class), new JournalEntryPersistenceConverter());
                Instant start = Instant.parse("2026-11-14T00:00:00Z"), end = start.plusSeconds(60);
                var first = repository.findInPostedWindow(start, end, 0, 1).getFirst();
                assertEquals(1L, first.journalEntry().getJournalEntryId().value());
                assertEquals(3L, first.version());
                assertNotNull(first.updatedAt());
                assertEquals(11L, first.journalEntry().getPostings().getFirst().getPostingId().value());
                assertEquals(2L, repository.findInPostedWindow(start, end, 1, 1).getFirst().journalEntry().getJournalEntryId().value());
                assertTrue(repository.findInPostedWindow(start, end, 2, 1).isEmpty());
                var postings = repository.findPostingsByJournalEntryIds(List.of(JournalEntryId.of(1L), JournalEntryId.of(2L)));
                assertEquals(2, postings.size());
                assertEquals(21L, postings.get(JournalEntryId.of(2L)).getFirst().posting().getPostingId().value());
                assertEquals(5, postings.get(JournalEntryId.of(2L)).getFirst().sequenceNo());
                assertEquals(9, postings.get(JournalEntryId.of(2L)).getLast().sequenceNo());
                assertTrue(repository.findPostingsByJournalEntryIds(List.of()).isEmpty());
                assertThrows(IllegalArgumentException.class, () -> repository.findInPostedWindow(start, start, 0, 10));
                try (var statement = connection.createStatement()) {
                    statement.execute("UPDATE ledger_journal_entry SET posted_at = '2026-11-14T00:00:00.000300Z' WHERE id = 1");
                }
                session.clearCache();
                assertTrue(repository.findInPostedWindow(start.plusNanos(400000), start.plusNanos(500000), 0, 10).isEmpty());
                session.rollback(true);
            }
        }
    }
}
