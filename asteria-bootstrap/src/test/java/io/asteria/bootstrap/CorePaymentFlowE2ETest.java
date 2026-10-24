package io.asteria.bootstrap;

import io.asteria.payment.application.request.ConsumeBalanceRequest;
import com.fasterxml.jackson.databind.JsonNode;
import io.asteria.payment.application.request.ReleaseBalanceRequest;
import io.asteria.payment.application.request.ReserveBalanceRequest;
import io.asteria.balance.application.command.CreateBalanceCommand;
import io.asteria.balance.application.command.CreditBalanceCommand;
import io.asteria.balance.application.service.BalanceAccountApplicationService;
import io.asteria.balance.application.service.BalanceApplicationService;
import io.asteria.balance.domain.enums.BalanceAccountOwnerType;
import io.asteria.common.application.port.DistributedIdGenerator;
import io.asteria.common.domain.valueobject.CurrencyCode;
import io.asteria.common.domain.valueobject.Money;
import io.asteria.common.util.JsonUtils;
import io.asteria.ledger.application.command.CreateLedgerAccountCommand;
import io.asteria.ledger.application.service.LedgerAccountApplicationService;
import io.asteria.ledger.domain.enums.LedgerAccountCategory;
import io.asteria.ledger.domain.enums.LedgerAccountOwnerType;
import io.asteria.payment.application.port.channel.AuthorizationResult;
import io.asteria.payment.application.port.channel.CaptureResult;
import io.asteria.payment.infrastructure.channel.CardPaymentChannel;
import io.asteria.payment.infrastructure.client.BalanceClient;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

/** Real HTTP/Feign, PostgreSQL, transactional outboxes, Kafka and Ledger. Only the external card channel is controlled. */
@EnabledIfSystemProperty(named = "asteria.e2e", matches = "true")
@SpringBootTest(classes = AsteriaApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "asteria.service.balance.url=http://localhost:${local.server.port}",
                "asteria.service.ledger.url=http://localhost:${local.server.port}",
                "spring.cloud.openfeign.lazy-attributes-resolution=true",
                "spring.main.lazy-initialization=true",
                "asteria.payment.outbox.publish-task-enabled=false",
                "asteria.settlement.outbox.publish-task-enabled=false",
                "spring.kafka.consumer.auto-offset-reset=earliest",
                "spring.kafka.listener.ack-mode=record",
                "logging.level.org.apache.kafka=WARN",
                "logging.level.kafka=WARN"
        })
@EmbeddedKafka(partitions = 1, topics = {"payment-captured", "settlement-completed"},
        bootstrapServersProperty = "spring.kafka.bootstrap-servers")
@DirtiesContext
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CorePaymentFlowE2ETest {
    private static final CurrencyCode USD = CurrencyCode.of("USD");
    private static final String SCHEMA = "core_e2e_" + UUID.randomUUID().toString().replace("-", "");
    private static final String URL = env("ASTERIA_E2E_JDBC_URL", "jdbc:postgresql://localhost:5432/postgres");
    private static final String USER = env("ASTERIA_E2E_DB_USER", "postgres");
    private static final String PASSWORD = env("ASTERIA_E2E_DB_PASSWORD", "");
    private static final List<String> TABLES = List.of("balance_account", "balance", "balance_reservation",
            "balance_movement", "payment", "payment_outbox_event", "ledger_account", "ledger_journal_entry",
            "ledger_posting", "settlement_batch", "settlement_item", "settlement_outbox_event");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry properties) throws Exception {
        // Only this generated schema is in search_path: legacy DDL DROP TABLE statements cannot touch public.
        try (var connection = DriverManager.getConnection(URL, USER, PASSWORD);
             var statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA " + SCHEMA);
            statement.execute("SET search_path TO " + SCHEMA);
            for (String table : TABLES) {
                ScriptUtils.executeSqlScript(connection, new ClassPathResource("sql/" + table + ".sql"));
            }
        }
        properties.add("spring.datasource.url", () -> URL + (URL.contains("?") ? "&" : "?") + "currentSchema=" + SCHEMA);
        properties.add("spring.datasource.username", () -> USER);
        properties.add("spring.datasource.password", () -> PASSWORD);
    }

    @LocalServerPort private int port;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private DistributedIdGenerator ids;
    @Autowired private BalanceAccountApplicationService accounts;
    @Autowired private BalanceApplicationService balances;
    @Autowired private LedgerAccountApplicationService ledgerAccounts;
    @Autowired private BalanceClient balanceClient;
    @Autowired private io.asteria.payment.application.publisher.PaymentOutboxPublisher paymentPublisher;
    @Autowired private io.asteria.settlement.application.publisher.SettlementOutboxPublisher settlementPublisher;
    @Autowired private KafkaTemplate<String, String> kafka;
    @Autowired private EmbeddedKafkaBroker broker;
    @Autowired private KafkaListenerEndpointRegistry listeners;
    // Initialize listener beans even with lazy Feign initialization for the random HTTP port.
    @Autowired private io.asteria.ledger.infrastructure.messaging.consumer.PaymentCapturedConsumer paymentConsumer;
    @Autowired private io.asteria.ledger.infrastructure.messaging.consumer.SettlementCompletedConsumer settlementConsumer;
    @MockitoSpyBean private CardPaymentChannel channel;
    private final Set<Long> declinedPayments = ConcurrentHashMap.newKeySet();
    private RestClient http;

    @BeforeAll
    void infrastructure() {
        http = RestClient.builder().baseUrl("http://localhost:" + port).build();
        assertThat(listeners.getListenerContainers()).hasSize(2);
        for (var container : listeners.getListenerContainers()) {
            ContainerTestUtils.waitForAssignment(container, 1);
        }
        createLedgerAccount("PAYMENT_RECEIVABLE_USD", LedgerAccountCategory.ASSET);
        createLedgerAccount("SETTLEMENT_PAYABLE_USD", LedgerAccountCategory.LIABILITY);
        createLedgerAccount("BANK_CASH_USD", LedgerAccountCategory.ASSET);
        createLedgerAccount("PROCESSING_FEE_EXPENSE_USD", LedgerAccountCategory.EXPENSE);
    }

    @BeforeEach
    void externalChannel() {
        // Inspect committed DB state from the channel boundary: reserve must precede the external authorization.
        doAnswer(call -> {
            var request = (io.asteria.payment.application.port.channel.AuthorizationRequest) call.getArgument(0);
            long paymentId = request.paymentId().value();
            assertBalanceForPayment(paymentId, "900", "100", 2);
            assertReservation(paymentId, "RESERVED");
            assertMovements(paymentId, "RESERVE");
            return declinedPayments.contains(paymentId)
                    ? AuthorizationResult.failure("DECLINED", "Explicit E2E authorization decline")
                    : AuthorizationResult.success("e2e-auth-" + paymentId);
        }).when(channel).authorize(any());
        doAnswer(call -> {
            var request = (io.asteria.payment.application.port.channel.CaptureRequest) call.getArgument(0);
            return CaptureResult.success("e2e-capture-" + request.paymentId().value());
        }).when(channel).capture(any());
    }

    @Test
    void successFromPaymentThroughSettlementPostsBothJournals() throws Exception {
        long paymentId = capturedPayment();
        Map<String, Object> paymentEvent = paymentOutbox(paymentId);
        assertOutbox(paymentEvent, "PENDING", "PAYMENT_CAPTURED", paymentId);
        paymentPublisher.publishPendingEvents();
        awaitJournal(paymentEvent, "PAYMENT", paymentId);
        assertPaymentPostings(paymentEvent);
        assertOutbox(paymentOutbox(paymentId), "PUBLISHED", "PAYMENT_CAPTURED", paymentId);

        String reference = "e2e-settlement-" + paymentId;
        long batchId = createdId(post("/api/settlements/batches", Map.of(
                "reference", reference, "grossAmount", money("100"), "feeAmount", money("3"), "netAmount", money("97"),
                "items", List.of(Map.of("paymentId", paymentId, "paymentReference", paymentReference(paymentId), "amount", money("100"))))));
        assertThat(row("SELECT * FROM settlement_batch WHERE id = ?", batchId).get("status")).isEqualTo("CREATED");
        post("/api/settlements/batches/" + batchId + "/submit", Map.of());
        var accepted = row("SELECT * FROM settlement_batch WHERE id = ?", batchId);
        assertThat(accepted.get("status")).isEqualTo("ACCEPTED");
        assertThat(accepted.get("processing_at")).isNotNull();
        assertThat(accepted.get("accepted_at")).isNotNull();
        post("/api/settlements/batches/complete", Map.of(
                "channelSettlementBatchId", accepted.get("channel_settlement_batch_id"), "settledAt", Instant.now().toString()));
        var settled = row("SELECT * FROM settlement_batch WHERE id = ?", batchId);
        assertThat(settled.get("status")).isEqualTo("SETTLED");
        amount(settled, "gross_amount", "100");
        amount(settled, "fee_amount", "3");
        amount(settled, "net_amount", "97");
        var item = row("SELECT * FROM settlement_item WHERE settlement_batch_id = ?", batchId);
        assertThat(((Number) item.get("payment_id")).longValue()).isEqualTo(paymentId);
        assertThat(item.get("payment_reference")).isEqualTo(paymentReference(paymentId));
        amount(item, "amount", "100");
        assertThat(item.get("currency")).isEqualTo("USD");
        var event = row("SELECT * FROM settlement_outbox_event WHERE aggregate_id = ?", Long.toString(batchId));
        assertOutbox(event, "PENDING", "SETTLEMENT_COMPLETED", batchId);
        settlementPublisher.publishPendingEvents();
        awaitJournal(event, "SETTLEMENT_BATCH", batchId);
        assertPostings(event, Map.of("BANK_CASH_USD:DEBIT", "97", "PROCESSING_FEE_EXPENSE_USD:DEBIT", "3",
                "PAYMENT_RECEIVABLE_USD:CREDIT", "100")); // Existing Settlement rule clears the receivable.
        assertOutbox(row("SELECT * FROM settlement_outbox_event WHERE event_id = ?", event.get("event_id")),
                "PUBLISHED", "SETTLEMENT_COMPLETED", batchId);
        assertBalanceForPayment(paymentId, "900", "0", 3);
        // The receivable clears and both journals balance; the merchant payable remains outstanding in v1.
        assertThat(jdbc.queryForObject("SELECT COALESCE(SUM(CASE p.direction WHEN 'DEBIT' THEN p.amount ELSE -p.amount END),0) "
                + "FROM ledger_posting p JOIN ledger_account a ON a.id=p.ledger_account_id "
                + "JOIN ledger_journal_entry j ON j.id=p.journal_entry_id "
                + "WHERE a.account_code='PAYMENT_RECEIVABLE_USD' AND j.event_id IN (?,?)", BigDecimal.class,
                paymentEvent.get("event_id"), event.get("event_id"))).isEqualByComparingTo("0");
        replayAndAwait(event, "settlement-completed", "asteria-ledger-settlement-completed");
        assertPostings(event, Map.of("BANK_CASH_USD:DEBIT", "97", "PROCESSING_FEE_EXPENSE_USD:DEBIT", "3",
                "PAYMENT_RECEIVABLE_USD:CREDIT", "100"));
    }

    @Test
    void explicitAuthorizationFailureReleasesReservedFundsAndProducesNoCaptureEvent() {
        long paymentId = createdPayment();
        declinedPayments.add(paymentId);
        post("/api/payment/" + paymentId + "/authorize", Map.of());
        assertThat(payment(paymentId).get("status")).isEqualTo("FAILED");
        assertBalanceForPayment(paymentId, "1000", "0", 3);
        assertReservation(paymentId, "RELEASED");
        assertMovements(paymentId, "RESERVE", "RELEASE");
        var before = balanceForPayment(paymentId);
        assertThat(balanceClient.release(new ReleaseBalanceRequest(merchant(paymentId), "USD", "ORDER",
                paymentReference(paymentId), paymentId)).success()).isTrue();
        assertThat(balanceForPayment(paymentId)).isEqualTo(before);
        assertMovements(paymentId, "RESERVE", "RELEASE");
        paymentPublisher.publishPendingEvents();
        assertThat(count("SELECT COUNT(*) FROM payment_outbox_event WHERE aggregate_id = ?", Long.toString(paymentId))).isZero();
        assertThat(count("SELECT COUNT(*) FROM ledger_journal_entry WHERE source_type='PAYMENT' AND source_id = ?",
                "PAYMENT_" + paymentId)).isZero();
        assertThat(payment(paymentId).get("authorized_at")).isNull();
        assertThat(payment(paymentId).get("captured_at")).isNull();
    }

    @Test
    void repeatedBalanceRequestsAndKafkaDeliveryDoNotMoveFundsOrPostAgain() throws Exception {
        long paymentId = capturedPayment();
        var event = paymentOutbox(paymentId);
        paymentPublisher.publishPendingEvents();
        awaitJournal(event, "PAYMENT", paymentId);
        var before = balanceForPayment(paymentId);
        var reservation = reservation(paymentId);
        var movements = paymentMovements(paymentId);
        for (int retry = 0; retry < 3; retry++) {
            // The exact event/reference originally sent by Payment, through its real Feign client.
            assertThat(balanceClient.reserve(new ReserveBalanceRequest(merchant(paymentId), "USD", new BigDecimal("100"),
                    "ORDER", paymentReference(paymentId), paymentId)).success()).isTrue();
            assertThat(balanceClient.consume(new ConsumeBalanceRequest(merchant(paymentId), "USD", "ORDER",
                    paymentReference(paymentId), paymentId)).success()).isTrue();
        }
        replayAndAwait(event, "payment-captured", "asteria-ledger-payment-captured");
        assertThat(balanceForPayment(paymentId)).isEqualTo(before);
        assertThat(reservation(paymentId)).isEqualTo(reservation);
        assertThat(paymentMovements(paymentId)).isEqualTo(movements);
        assertThat(payment(paymentId).get("status")).isEqualTo("CAPTURED");
        assertThat(count("SELECT COUNT(*) FROM payment_outbox_event WHERE aggregate_id = ?", Long.toString(paymentId))).isEqualTo(1);
        assertPaymentPostings(event);
    }

    private long createdPayment() {
        long merchantId = ids.nextId();
        var accountId = accounts.create(BalanceAccountOwnerType.MERCHANT, merchantId);
        balances.create(CreateBalanceCommand.builder().balanceAccountId(accountId).currency(USD).build());
        balances.credit(CreditBalanceCommand.builder().balanceAccountId(accountId).currency(USD).amount(money("1000"))
                .referenceType("E2E_FUNDING").referenceId(Long.toString(merchantId)).eventId("fund-" + merchantId).build());
        long id = createdId(post("/api/payment", Map.of("merchantId", merchantId, "amount", money("100"),
                "paymentMethod", "CARD", "reference", Map.of("referenceType", "ORDER", "referenceId", "e2e-" + merchantId))));
        assertThat(payment(id).get("status")).isEqualTo("CREATED");
        assertBalanceForPayment(id, "1000", "0", 1);
        return id;
    }

    private long capturedPayment() {
        long id = createdPayment();
        post("/api/payment/" + id + "/authorize", Map.of());
        assertThat(payment(id).get("status")).isEqualTo("AUTHORIZED");
        assertBalanceForPayment(id, "900", "100", 2);
        assertReservation(id, "RESERVED");
        assertMovements(id, "RESERVE");
        // Retry reserve while authorization is still live as well as after consume in the idempotency test.
        assertThat(balanceClient.reserve(new ReserveBalanceRequest(merchant(id), "USD", new BigDecimal("100"),
                "ORDER", paymentReference(id), id)).success()).isTrue();
        assertBalanceForPayment(id, "900", "100", 2);
        assertMovements(id, "RESERVE");
        post("/api/payment/" + id + "/capture", Map.of());
        assertThat(payment(id).get("status")).isEqualTo("CAPTURED");
        assertBalanceForPayment(id, "900", "0", 3);
        assertReservation(id, "CONSUMED");
        assertMovements(id, "RESERVE", "DEBIT_RESERVED");
        return id;
    }

    private void replayAndAwait(Map<String, Object> event, String topic, String group) throws Exception {
        try (var admin = AdminClient.create(Map.of("bootstrap.servers", broker.getBrokersAsString()))) {
            for (int retry = 0; retry < 2; retry++) {
                var metadata = kafka.send(topic, event.get("aggregate_id").toString(), event.get("payload").toString())
                        .get(10, TimeUnit.SECONDS).getRecordMetadata();
                // A DB count alone can pass before a duplicate has even been consumed. Require committed offset.
                await().atMost(Duration.ofSeconds(45)).untilAsserted(() -> {
                    var offsets = admin.listConsumerGroupOffsets(group).partitionsToOffsetAndMetadata().get(5, TimeUnit.SECONDS);
                    var committed = offsets.get(new TopicPartition(topic, metadata.partition()));
                    assertThat(committed).isNotNull();
                    assertThat(committed.offset()).isGreaterThan(metadata.offset());
                });
            }
        }
        assertThat(count("SELECT COUNT(*) FROM ledger_journal_entry WHERE event_id = ?", event.get("event_id"))).isEqualTo(1);
    }

    private void awaitJournal(Map<String, Object> event, String sourceType, long sourceId) {
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            var journals = jdbc.queryForList("SELECT * FROM ledger_journal_entry WHERE event_id = ?", event.get("event_id"));
            assertThat(journals).hasSize(1);
            var journal = journals.getFirst();
            assertThat(journal.get("status")).isEqualTo("POSTED");
            assertThat(journal.get("posted_at")).isNotNull();
            assertThat(journal.get("source_type")).isEqualTo(sourceType);
            assertThat(journal.get("source_id")).isEqualTo((sourceType.equals("PAYMENT") ? "PAYMENT_" : "SETTLEMENT_") + sourceId);
            assertThat(journal.get("event_type")).isEqualTo(event.get("event_type"));
            String table = sourceType.equals("PAYMENT") ? "payment_outbox_event" : "settlement_outbox_event";
            assertThat(row("SELECT * FROM " + table + " WHERE event_id = ?", event.get("event_id")).get("status")).isEqualTo("PUBLISHED");
        });
    }

    private void assertPaymentPostings(Map<String, Object> event) {
        assertPostings(event, Map.of("PAYMENT_RECEIVABLE_USD:DEBIT", "100", "SETTLEMENT_PAYABLE_USD:CREDIT", "100"));
    }

    private void assertPostings(Map<String, Object> event, Map<String, String> expected) {
        assertThat(count("SELECT COUNT(*) FROM ledger_journal_entry WHERE event_id = ?", event.get("event_id"))).isEqualTo(1);
        var postings = jdbc.queryForList("SELECT a.account_code,p.* FROM ledger_posting p JOIN ledger_account a "
                + "ON a.id=p.ledger_account_id JOIN ledger_journal_entry j ON j.id=p.journal_entry_id WHERE j.event_id=? ORDER BY p.sequence_no",
                event.get("event_id"));
        assertThat(postings).hasSize(expected.size());
        assertThat(postings.stream().map(p -> p.get("account_code") + ":" + p.get("direction")).toList())
                .containsExactlyInAnyOrderElementsOf(expected.keySet());
        BigDecimal total = BigDecimal.ZERO;
        for (int i = 0; i < postings.size(); i++) {
            var posting = postings.get(i);
            String key = posting.get("account_code") + ":" + posting.get("direction");
            assertThat(expected).containsKey(key);
            amount(posting, "amount", expected.get(key));
            assertThat(posting.get("currency")).isEqualTo("USD");
            assertThat(((Number) posting.get("sequence_no")).intValue()).isEqualTo(i + 1);
            BigDecimal value = (BigDecimal) posting.get("amount");
            total = total.add(posting.get("direction").equals("DEBIT") ? value : value.negate());
        }
        assertThat(total).isEqualByComparingTo("0");
    }

    private void assertBalanceForPayment(long id, String available, String reserved, long version) {
        var balance = balanceForPayment(id);
        amount(balance, "available_amount", available);
        amount(balance, "reserved_amount", reserved);
        assertThat(balance.get("currency")).isEqualTo("USD");
        assertThat(((Number) balance.get("version")).longValue()).isEqualTo(version);
    }

    private void assertReservation(long id, String status) {
        var reservation = reservation(id);
        assertThat(reservation.get("status")).isEqualTo(status);
        assertThat(reservation.get("reference_type")).isEqualTo("ORDER");
        assertThat(reservation.get("reference_id")).isEqualTo(paymentReference(id));
        assertThat(reservation.get("currency")).isEqualTo("USD");
        amount(reservation, "amount", "100");
    }

    private void assertMovements(long id, String... types) {
        var movements = paymentMovements(id);
        assertThat(movements.stream().map(m -> m.get("movement_type")).toList()).containsExactly((Object[]) types);
        for (var movement : movements) {
            amount(movement, "amount", "100");
            assertThat(movement.get("currency")).isEqualTo("USD");
            assertThat(movement.get("reference_type")).isEqualTo("ORDER");
            assertThat(movement.get("reference_id")).isEqualTo(paymentReference(id));
            assertThat(movement.get("event_id")).isEqualTo(Long.toString(id));
        }
    }

    private void assertOutbox(Map<String, Object> event, String status, String type, long aggregateId) {
        assertThat(event.get("event_id")).isInstanceOf(String.class).asString().isNotBlank();
        assertThat(event.get("status")).isEqualTo(status);
        assertThat(event.get("event_type")).isEqualTo(type);
        assertThat(event.get("aggregate_id")).isEqualTo(Long.toString(aggregateId));
        assertThat(JsonUtils.readTree(event.get("payload").toString()).path("eventId").asText()).isEqualTo(event.get("event_id"));
        if (status.equals("PUBLISHED")) assertThat(event.get("published_at")).isNotNull();
        else assertThat(event.get("published_at")).isNull();
    }

    @AfterEach
    void writeDatabaseEvidence(TestInfo info) throws Exception {
        var evidence = new LinkedHashMap<String, Object>();
        evidence.put("scenario", info.getTestMethod().orElseThrow().getName());
        evidence.put("schema", SCHEMA);
        for (String table : TABLES) evidence.put(table, jdbc.queryForList("SELECT * FROM " + table + " ORDER BY id"));
        Path directory = Path.of("target", "core-payment-flow-e2e");
        Files.createDirectories(directory);
        String json = JsonUtils.toPrettyJson(evidence);
        Files.writeString(directory.resolve(info.getTestMethod().orElseThrow().getName() + ".json"), json);
        System.out.println("CORE_PAYMENT_FLOW_E2E schema=" + SCHEMA + " evidence="
                + directory.resolve(info.getTestMethod().orElseThrow().getName() + ".json").toAbsolutePath());
    }

    @AfterAll
    void cleanup() throws Exception {
        listeners.stop();
        if (!Boolean.getBoolean("asteria.e2e.keep-schema")) {
            try (var connection = DriverManager.getConnection(URL, USER, PASSWORD); var statement = connection.createStatement()) {
                statement.execute("DROP SCHEMA " + SCHEMA + " CASCADE");
            }
        }
    }

    private void createLedgerAccount(String code, LedgerAccountCategory category) {
        ledgerAccounts.createLedgerAccount(new CreateLedgerAccountCommand(code, LedgerAccountOwnerType.PLATFORM, 1L, category, USD, false));
    }
    private JsonNode post(String path, Object body) {
        String response = http.post().uri(path).contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .body(JsonUtils.toJson(body)).retrieve().body(String.class);
        var tree = JsonUtils.readTree(response);
        assertThat(tree.path("success").asBoolean()).as(response).isTrue();
        return tree;
    }
    private long createdId(JsonNode response) {
        assertThat(response.path("data").path("value").isIntegralNumber()).isTrue();
        return response.path("data").path("value").longValue();
    }
    private Map<String, Object> row(String sql, Object... args) { return jdbc.queryForMap(sql, args); }
    private long count(String sql, Object... args) { return Objects.requireNonNull(jdbc.queryForObject(sql, Long.class, args)); }
    private Map<String, Object> payment(long id) { return row("SELECT * FROM payment WHERE id = ?", id); }
    private long merchant(long id) { return ((Number) payment(id).get("merchant_id")).longValue(); }
    private String paymentReference(long id) { return payment(id).get("reference_id").toString(); }
    private Map<String, Object> paymentOutbox(long id) { return row("SELECT * FROM payment_outbox_event WHERE aggregate_id = ?", Long.toString(id)); }
    private Map<String, Object> balanceForPayment(long id) {
        return row("SELECT b.* FROM balance b JOIN balance_account a ON a.id=b.balance_account_id WHERE a.owner_type='MERCHANT' AND a.owner_id=? AND b.currency='USD'", merchant(id));
    }
    private Map<String, Object> reservation(long id) { return row("SELECT * FROM balance_reservation WHERE balance_id=? AND reference_type='ORDER' AND reference_id=?", balanceForPayment(id).get("id"), paymentReference(id)); }
    private List<Map<String, Object>> paymentMovements(long id) { return jdbc.queryForList("SELECT * FROM balance_movement WHERE balance_id=? AND event_id=? ORDER BY id", balanceForPayment(id).get("id"), Long.toString(id)); }
    private static Money money(String amount) { return Money.of(new BigDecimal(amount), USD); }
    private static void amount(Map<String, Object> row, String column, String value) { assertThat((BigDecimal) row.get(column)).isEqualByComparingTo(value); }
    private static String env(String key, String fallback) { return System.getenv().getOrDefault(key, fallback); }
}
