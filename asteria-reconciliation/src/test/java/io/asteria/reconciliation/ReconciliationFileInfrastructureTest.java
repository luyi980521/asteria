package io.asteria.reconciliation;

import io.asteria.reconciliation.application.model.*;
import io.asteria.reconciliation.infrastructure.file.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证文件协议、原始字节完整性及本地原子发布，不执行任何对账业务。
 */
class ReconciliationFileInfrastructureTest {
    @TempDir Path directory;
    private final ReconciliationCsvWriter writer = new ReconciliationCsvWriter();
    private final ReconciliationCsvReader reader = new ReconciliationCsvReader();
    private static final Instant NOW = Instant.parse("2026-11-14T00:00:00.123456789Z");

    @Test
    void allThreeDtosRoundTripIncludingSpecialCharactersNullAndEmpty() throws Exception {
        var first = PaymentCaptureExportRecord.builder().outboxId(1L).eventId("capture-1").paymentId(42L)
                .referenceType("ORDER").referenceId("订单,\"甲\"\r\n次行\n尾行").eventType("PAYMENT_CAPTURED")
                .amount(new BigDecimal("10.000000000000000001")).currency("USD")
                .publicationStatus("PENDING").capturedAt(NOW).createdAt(NOW).traceId("").build();
        var second = PaymentCaptureExportRecord.builder().outboxId(2L).eventId("capture-2").paymentId(42L)
                .amount(new BigDecimal("20.00")).currency("USD").capturedAt(NOW).build();
        assertRoundTrip(PaymentCaptureExportRecord.class, List.of(first, second));
        var journal = LedgerJournalExportRecord.builder().journalEntryId(50L).sourceType("PAYMENT")
                .sourceId("pay_42").eventId("capture-2").status("POSTED").postedAt(NOW)
                .reversalReason("逗号,和\"引号\"\n换行").sourceVersion(3L).createdAt(NOW).updatedAt(NOW).build();
        assertRoundTrip(LedgerJournalExportRecord.class, List.of(journal));
        var posting = LedgerPostingExportRecord.builder().postingId(60L).journalEntryId(50L).sequenceNo(1)
                .ledgerAccountId(70L).amount(new BigDecimal("0.01")).currency("USD").direction("DEBIT").build();
        assertRoundTrip(LedgerPostingExportRecord.class, List.of(posting));
        assertRoundTrip(LedgerPostingExportRecord.class, List.of());
        assertRoundTrip(SettlementCompletedExportRecord.class, List.of(SettlementCompletedExportRecord.builder()
                .eventId("settlement-1").settlementBatchId(5L).settlementReference("渠道,\"甲\"\n批次")
                .grossAmount(new BigDecimal("10.00")).grossCurrency("USD")
                .feeAmount(new BigDecimal("0.00")).feeCurrency("USD")
                .netAmount(new BigDecimal("10.00")).netCurrency("USD").settledAt(NOW).build()));
    }

    private <T> void assertRoundTrip(Class<T> type, List<T> values) throws Exception {
        var bytes = new ByteArrayOutputStream();
        assertEquals(values.size(), writer.write(bytes, type, values));
        assertTrue(bytes.toString(StandardCharsets.UTF_8).endsWith("\r\n"));
        List<T> restored = new ArrayList<>();
        assertEquals(values.size(), reader.read(new ByteArrayInputStream(bytes.toByteArray()), type, restored::add));
        assertEquals(values, restored);
    }

    @Test
    void rejectsMalformedCsvWrongHeaderColumnsValuesAndUtf8() throws Exception {
        var bytes = new ByteArrayOutputStream();
        writer.write(bytes, LedgerPostingExportRecord.class, List.of());
        String header = bytes.toString(StandardCharsets.UTF_8);
        for (String content : List.of("wrong\r\n", header + "\"unclosed", header + "a\"b,1,1,1,1,USD,DEBIT\r\n",
                header + "\"a\"x,1,1,1,1,USD,DEBIT\r\n", header + "1,2\r\n",
                header + "x,1,1,1,1,USD,DEBIT\r\n")) {
            assertThrows(IOException.class, () -> reader.read(stream(content), LedgerPostingExportRecord.class, value -> { }));
        }
        byte[] invalid = Arrays.copyOf(bytes.toByteArray(), bytes.size() + 1);
        invalid[invalid.length - 1] = (byte) 0xff;
        assertThrows(IOException.class, () -> reader.read(new ByteArrayInputStream(invalid), LedgerPostingExportRecord.class, value -> { }));
        List<LedgerPostingExportRecord> restored = new ArrayList<>();
        assertEquals(1, reader.read(stream("\ufeff" + header + "1,2,3,4,1.00,USD,DEBIT"), LedgerPostingExportRecord.class, restored::add));
    }

    @Test
    void checksumMatchesKnownVectorAndDetectsCorruption() throws Exception {
        String expected = "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad";
        assertEquals(expected, FileChecksumUtils.sha256(stream("abc")));
        FileChecksumUtils.verify(stream("abc"), expected);
        assertThrows(IOException.class, () -> FileChecksumUtils.verify(stream("abd"), expected));
        assertThrows(IllegalArgumentException.class, () -> FileChecksumUtils.verify(stream("abc"), "bad"));
    }

    @Test
    void objectStoragePublishesOnlyCompleteObjectsAndCleansFailedUploads() throws Exception {
        var storage = new LocalObjectStorageClient(directory.toString());
        String key = "payment/2026-11-14/captures.csv";
        assertFalse(storage.exists(key));
        assertThrows(IOException.class, () -> storage.get(key));
        InputStream firstUpload = new InputStream() {
            @Override public int read() throws IOException {
                assertFalse(storage.exists(key));
                throw new IOException("Simulated first upload interruption");
            }
        };
        assertThrows(IOException.class, () -> storage.put(key, firstUpload));
        assertFalse(storage.exists(key));
        storage.put(key, stream("old"));
        InputStream interrupted = new InputStream() {
            @Override public int read() throws IOException {
                try (var old = storage.get(key)) { assertEquals("old", new String(old.readAllBytes(), StandardCharsets.UTF_8)); }
                throw new IOException("Simulated upload interruption");
            }
        };
        assertThrows(IOException.class, () -> storage.put(key, interrupted));
        try (var files = Files.walk(directory)) {
            assertEquals(1, files.filter(Files::isRegularFile).count());
        }
        storage.put(key, stream("新文件\r\n"));
        try (var content = storage.get(key)) {
            assertEquals("新文件\r\n", new String(content.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    @Test
    void rejectsTraversalAbsoluteAndInvalidKeysForEveryOperation() throws Exception {
        var storage = new LocalObjectStorageClient(directory.toString());
        for (String key : List.of("../escape", "a/../escape", "a/./b", "/absolute", "C:/file", "a\\b", "a//b", "a/", "", ".upload-hidden", "a./b", "NUL", "a/CON.csv", "a?/b")) {
            assertThrows(IllegalArgumentException.class, () -> storage.put(key, stream("x")), key);
            assertThrows(IllegalArgumentException.class, () -> storage.get(key), key);
            assertThrows(IllegalArgumentException.class, () -> storage.exists(key), key);
        }
    }

    @Test
    void manifestUsesSharedJsonAndIsDeliveredAsSeparateObject() throws Exception {
        var file = ReconciliationFileMetadata.builder().fileId(1L).objectKey("capture/data.csv")
                .sourceSystem("PAYMENT").recordType("PAYMENT_CAPTURE").windowStart(NOW.minusSeconds(60)).windowEnd(NOW)
                .fileVersion(0L).schemaVersion(1).recordCount(0L).byteSize(3L)
                .sha256(FileChecksumUtils.sha256(stream("abc"))).createdAt(NOW)
                .businessDate(java.time.LocalDate.of(2026, 11, 14)).cutoffAt(NOW).build();
        var manifest = ReconciliationFileManifest.builder().schemaVersion(1).deliveryId(2L).createdAt(NOW)
                .sourceSystem("PAYMENT").businessDate(file.businessDate()).windowStart(file.windowStart()).windowEnd(file.windowEnd())
                .cutoffAt(NOW).fileVersion(0L).recordCount(0L).files(List.of(file)).build();
        var serializer = new ManifestSerializer();
        var storage = new LocalObjectStorageClient(directory.toString());
        storage.put(file.objectKey(), stream("abc"));
        storage.put("capture/manifest.json", new ByteArrayInputStream(serializer.serialize(manifest)));
        try (var input = storage.get("capture/manifest.json")) {
            assertEquals(manifest, serializer.deserialize(input.readAllBytes()));
        }
        try (var input = storage.get(file.objectKey())) { FileChecksumUtils.verify(input, file.sha256()); }
        assertEquals(3, storage.metadata(file.objectKey()).byteSize());
        assertEquals(1, storage.list("capture/", null, 1).objects().size());
        var page = storage.list("capture/", null, 1);
        assertNotNull(page.nextAfterKey());
        assertNull(storage.list("capture/", page.nextAfterKey(), 1).nextAfterKey());
    }

    private static InputStream stream(String value) { return new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8)); }
}
