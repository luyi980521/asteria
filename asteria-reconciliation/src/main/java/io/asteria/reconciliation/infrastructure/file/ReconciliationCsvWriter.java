package io.asteria.reconciliation.infrastructure.file;

import org.springframework.stereotype.Component;
import java.io.*;
import java.lang.reflect.RecordComponent;
import java.nio.charset.StandardCharsets;

/**
 * 流式 UTF-8 CSV 写入；CRLF 分隔记录，所有非 null 值加引号，空字段表示 null，双引号空串表示空字符串。
 */
@Component
public class ReconciliationCsvWriter {
    /**
     * 按显式版本写入文件，不支持的版本在输出任何字节前失败。
     * @param output 调用方输出流
     * @param schemaVersion 数据格式版本
     * @param type 传输记录类型
     * @param records 数据记录
     * @param <T> 记录类型
     * @return 写入的数据记录数
     * @throws IOException 输出失败
     */
    public <T> long write(OutputStream output, Integer schemaVersion, Class<T> type, Iterable<T> records) throws IOException {
        if (!Integer.valueOf(FileSchemaValidator.SCHEMA_VERSION).equals(schemaVersion)) throw new IllegalArgumentException("Unsupported schemaVersion");
        return write(output, type, records);
    }
    /**
     * 写入表头及记录，返回不含表头的记录数；刷新但不关闭调用方输出流。
     */
    public <T> long write(OutputStream output, Class<T> type, Iterable<T> records) throws IOException {
        RecordComponent[] components = ReconciliationCsvSchema.components(type);
        Writer writer = new BufferedWriter(new OutputStreamWriter(output, StandardCharsets.UTF_8));
        writeRow(writer, ReconciliationCsvSchema.headers(type).toArray());
        long count = 0;
        for (T record : records) {
            if (record == null || !type.isInstance(record)) throw new IllegalArgumentException("Unexpected CSV record");
            Object[] values = new Object[components.length];
            try {
                for (int i = 0; i < components.length; i++) values[i] = components[i].getAccessor().invoke(record);
            } catch (ReflectiveOperationException exception) {
                throw new IOException("Cannot encode CSV record", exception);
            }
            writeRow(writer, values);
            count++;
        }
        writer.flush();
        return count;
    }

    private void writeRow(Writer writer, Object[] values) throws IOException {
        for (int i = 0; i < values.length; i++) {
            if (i > 0) writer.write(',');
            if (values[i] != null) {
                writer.write('"');
                writer.write(ReconciliationCsvSchema.format(values[i]).replace("\"", "\"\""));
                writer.write('"');
            }
        }
        writer.write("\r\n");
    }
}
