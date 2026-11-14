package io.asteria.reconciliation;

import io.asteria.reconciliation.application.model.*;
import io.asteria.reconciliation.infrastructure.file.*;
import io.asteria.common.util.JsonUtils;
import org.junit.jupiter.api.Test;
import java.io.*;
import java.time.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证文件协议结构与格式版本，不执行业务金额或差异判断。
 */
class FileSchemaValidatorTest {
    private static final Instant NOW = Instant.parse("2026-11-14T00:00:00Z");
    private final FileSchemaValidator validator = new FileSchemaValidator();

    private ReconciliationFileMetadata file(long id, String key, String recordType, long count) {
        return ReconciliationFileMetadata.builder().fileId(id).objectKey(key).sourceSystem("LEDGER")
                .recordType(recordType).businessDate(LocalDate.of(2026, 11, 13)).windowStart(NOW.minusSeconds(86400)).windowEnd(NOW)
                .cutoffAt(NOW.plusSeconds(60)).fileVersion(0L).schemaVersion(1).recordCount(count).byteSize(100L)
                .sha256("a".repeat(64)).createdAt(NOW.plusSeconds(120)).build();
    }

    private ReconciliationFileManifest manifest(int version, long count, List<ReconciliationFileMetadata> files) {
        var first = files.getFirst();
        return ReconciliationFileManifest.builder().schemaVersion(version).deliveryId(100L).createdAt(NOW.plusSeconds(120))
                .sourceSystem(first.sourceSystem()).businessDate(first.businessDate()).windowStart(first.windowStart()).windowEnd(first.windowEnd())
                .cutoffAt(first.cutoffAt()).fileVersion(first.fileVersion()).recordCount(count).files(files).build();
    }

    @Test
    void acceptsMultiFileManifestAndPreservesIndependentChecksums() {
        var files = List.of(file(1, "ledger/journal.csv", "LEDGER_JOURNAL", 10), file(2, "ledger/posting.csv", "LEDGER_POSTING", 20));
        var manifest = manifest(1, 30, files);
        var serializer = new ManifestSerializer();
        assertEquals(manifest, serializer.deserialize(serializer.serialize(manifest)));
        assertEquals(LedgerPostingExportRecord.class, validator.recordClass(1, "LEDGER_POSTING"));
        assertEquals(SettlementCompletedExportRecord.class, validator.recordClass(1, "SETTLEMENT_COMPLETED"));
    }

    @Test
    void rejectsUnsupportedVersionsDuplicatesCountsAndIncompleteMetadata() {
        var file = file(1, "ledger/journal.csv", "LEDGER_JOURNAL", 10);
        assertThrows(IllegalArgumentException.class, () -> validator.validate(manifest(2, 10, List.of(file))));
        assertThrows(IllegalArgumentException.class, () -> validator.validate(manifest(1, 9, List.of(file))));
        assertThrows(IllegalArgumentException.class, () -> validator.validate(manifest(1, 20, List.of(file, file))));
        assertThrows(IllegalArgumentException.class, () -> validator.validate(file(1, "../escape", "LEDGER_JOURNAL", 10)));
        assertThrows(IllegalArgumentException.class, () -> validator.recordClass(1, "UNKNOWN"));
        assertThrows(IllegalArgumentException.class, () -> validator.validate(ReconciliationFileMetadata.builder().build()));
        assertThrows(IllegalArgumentException.class, () -> new ManifestSerializer().deserialize(JsonUtils.toJsonBytes(manifest(99, 10, List.of(file)))));
    }

    @Test
    void rejectsUnsupportedCsvVersionBeforeWritingOrCallingConsumer() {
        var output = new ByteArrayOutputStream();
        assertThrows(IllegalArgumentException.class, () -> new ReconciliationCsvWriter().write(output, 2, LedgerJournalExportRecord.class, List.of()));
        assertEquals(0, output.size());
        assertThrows(IllegalArgumentException.class, () -> new ReconciliationCsvReader().read(new ByteArrayInputStream(new byte[0]), 2,
                LedgerJournalExportRecord.class, value -> fail("Unsupported version must not invoke consumer")));
    }
}
