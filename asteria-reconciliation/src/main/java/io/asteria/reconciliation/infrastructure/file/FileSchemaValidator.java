package io.asteria.reconciliation.infrastructure.file;

import io.asteria.reconciliation.application.model.*;
import org.springframework.stereotype.Component;
import java.util.HashSet;
import java.util.Objects;

/**
 * 文件协议结构校验；仅检查版本、必需元数据及清单内部一致性，不判断业务金额、账户或对账结果。
 */
@Component
public class FileSchemaValidator {
    /** 当前支持的文件协议版本 */
    public static final int SCHEMA_VERSION = 1;

    /**
     * 检查版本并返回对应 CSV 传输类型。
     * @param schemaVersion 文件格式版本
     * @param recordType 数据记录类型
     * @return 对应 DTO 类型
     * @throws IllegalArgumentException 未支持的格式或记录类型
     */
    public Class<?> recordClass(Integer schemaVersion, String recordType) {
        if (!Integer.valueOf(SCHEMA_VERSION).equals(schemaVersion)) throw invalid("Unsupported schemaVersion");
        if (recordType == null) throw invalid("Missing recordType");
        return switch (recordType) {
            case "PAYMENT_CAPTURE" -> PaymentCaptureExportRecord.class;
            case "LEDGER_JOURNAL" -> LedgerJournalExportRecord.class;
            case "LEDGER_POSTING" -> LedgerPostingExportRecord.class;
            case "SETTLEMENT_COMPLETED" -> SettlementCompletedExportRecord.class;
            default -> throw invalid("Unsupported recordType");
        };
    }

    /**
     * 校验单个文件描述，sha256 只检查格式，字节完整性使用 FileChecksumUtils。
     * @param file 文件描述
     * @throws IllegalArgumentException 描述缺失或结构非法
     */
    public void validate(ReconciliationFileMetadata file) {
        if (file == null) throw invalid("Missing file metadata");
        recordClass(file.schemaVersion(), file.recordType());
        ObjectStorageKeys.validate(file.objectKey());
        if (file.fileId() == null || file.fileId() <= 0 || file.sourceSystem() == null || file.sourceSystem().isBlank()
                || file.businessDate() == null || file.createdAt() == null || file.cutoffAt() == null
                || file.windowStart() == null || file.windowEnd() == null || !file.windowStart().isBefore(file.windowEnd())
                || file.fileVersion() == null || file.fileVersion() < 0 || file.recordCount() == null || file.recordCount() < 0
                || file.byteSize() == null || file.byteSize() < 0 || file.sha256() == null || !file.sha256().matches("[0-9a-f]{64}")) {
            throw invalid("Invalid file metadata");
        }
    }

    /**
     * 校验交付清单及各文件的结构关系；不访问存储、不导入来源记录。
     * @param manifest 交付清单
     * @throws IllegalArgumentException 版本、字段或文件描述不一致
     */
    public void validate(ReconciliationFileManifest manifest) {
        if (manifest == null || !Integer.valueOf(SCHEMA_VERSION).equals(manifest.schemaVersion())
                || manifest.deliveryId() == null || manifest.deliveryId() <= 0 || manifest.createdAt() == null
                || manifest.sourceSystem() == null || manifest.sourceSystem().isBlank() || manifest.businessDate() == null
                || manifest.windowStart() == null || manifest.windowEnd() == null || !manifest.windowStart().isBefore(manifest.windowEnd())
                || manifest.cutoffAt() == null || manifest.fileVersion() == null || manifest.fileVersion() < 0
                || manifest.recordCount() == null || manifest.recordCount() < 0 || manifest.files() == null || manifest.files().isEmpty()) {
            throw invalid("Invalid manifest");
        }
        var keys = new HashSet<String>();
        var ids = new HashSet<Long>();
        long count = 0;
        for (var file : manifest.files()) {
            validate(file);
            if (!keys.add(file.objectKey()) || !ids.add(file.fileId())
                    || !Objects.equals(manifest.sourceSystem(), file.sourceSystem())
                    || !Objects.equals(manifest.businessDate(), file.businessDate())
                    || !Objects.equals(manifest.windowStart(), file.windowStart()) || !Objects.equals(manifest.windowEnd(), file.windowEnd())
                    || !Objects.equals(manifest.cutoffAt(), file.cutoffAt()) || !Objects.equals(manifest.fileVersion(), file.fileVersion())
                    || !Objects.equals(manifest.schemaVersion(), file.schemaVersion())) throw invalid("Inconsistent manifest file metadata");
            try { count = Math.addExact(count, file.recordCount()); }
            catch (ArithmeticException exception) { throw invalid("Manifest recordCount overflow"); }
        }
        if (count != manifest.recordCount()) throw invalid("Manifest recordCount differs from file counts");
    }

    private IllegalArgumentException invalid(String reason) {
        return new IllegalArgumentException(reason);
    }
}
