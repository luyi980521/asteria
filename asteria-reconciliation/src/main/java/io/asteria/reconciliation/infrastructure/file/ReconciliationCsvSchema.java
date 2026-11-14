package io.asteria.reconciliation.infrastructure.file;

import io.asteria.reconciliation.application.model.*;
import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

/**
 * v1 固定列顺序由三个传输 record 的组件声明顺序定义，不接受任意 Java 类型。
 */
final class ReconciliationCsvSchema {
    private ReconciliationCsvSchema() { }

    static RecordComponent[] components(Class<?> type) {
        if (type != PaymentCaptureExportRecord.class && type != LedgerJournalExportRecord.class
                && type != LedgerPostingExportRecord.class && type != SettlementCompletedExportRecord.class) {
            throw new IllegalArgumentException("Unsupported reconciliation CSV type: " + type);
        }
        return type.getRecordComponents();
    }

    static List<String> headers(Class<?> type) {
        return Arrays.stream(components(type)).map(RecordComponent::getName).toList();
    }

    static Object parse(String text, Class<?> type) {
        if (text == null) return null;
        if (type == String.class) return text;
        if (type == Long.class) return Long.valueOf(text);
        if (type == Integer.class) return Integer.valueOf(text);
        if (type == BigDecimal.class) return new BigDecimal(text);
        if (type == Instant.class) return Instant.parse(text);
        throw new IllegalArgumentException("Unsupported CSV field type");
    }

    static String format(Object value) {
        return value instanceof BigDecimal decimal ? decimal.toPlainString() : value.toString();
    }
}
