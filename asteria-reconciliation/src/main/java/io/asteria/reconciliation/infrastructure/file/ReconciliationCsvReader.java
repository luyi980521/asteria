package io.asteria.reconciliation.infrastructure.file;

import org.springframework.stereotype.Component;
import java.io.*;
import java.lang.reflect.RecordComponent;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * 严格 UTF-8 CSV 读取；支持引号、逗号及字段内换行，按记录回调避免整文件驻留内存。
 */
@Component
public class ReconciliationCsvReader {
    /**
     * 按显式版本解析文件，不支持的版本在回调前失败。
     * @param input 调用方输入流
     * @param schemaVersion 数据格式版本
     * @param type 传输记录类型
     * @param consumer 每条记录的处理回调
     * @param <T> 记录类型
     * @return 解析的数据记录数
     * @throws IOException 文件格式或读取失败
     */
    public <T> long read(InputStream input, Integer schemaVersion, Class<T> type, Consumer<T> consumer) throws IOException {
        if (!Integer.valueOf(FileSchemaValidator.SCHEMA_VERSION).equals(schemaVersion)) throw new IllegalArgumentException("Unsupported schemaVersion");
        return read(input, type, consumer);
    }
    /**
     * 校验固定表头并逐行解析；不关闭输入流，回调异常直接传播以便调用方回滚批次。
     */
    public <T> long read(InputStream input, Class<T> type, Consumer<T> consumer) throws IOException {
        RecordComponent[] components = ReconciliationCsvSchema.components(type);
        var decoder = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        PushbackReader reader = new PushbackReader(new BufferedReader(new InputStreamReader(input, decoder)), 1);
        int first = reader.read();
        if (first != -1 && first != '\ufeff') reader.unread(first);
        List<String> header = row(reader);
        if (!ReconciliationCsvSchema.headers(type).equals(header)) throw new IOException("Unexpected CSV header");
        long count = 0;
        List<String> values;
        while ((values = row(reader)) != null) {
            if (values.size() != components.length) throw new IOException("CSV column count mismatch at record " + (count + 1));
            T record;
            try {
                Object[] arguments = new Object[components.length];
                for (int i = 0; i < arguments.length; i++) {
                    arguments[i] = ReconciliationCsvSchema.parse(values.get(i), components[i].getType());
                }
                record = type.getDeclaredConstructor(Arrays.stream(components).map(RecordComponent::getType)
                        .toArray(Class<?>[]::new)).newInstance(arguments);
            } catch (ReflectiveOperationException | IllegalArgumentException | java.time.DateTimeException exception) {
                throw new IOException("Invalid CSV value at record " + (count + 1), exception);
            }
            consumer.accept(record);
            count++;
        }
        return count;
    }

    private List<String> row(PushbackReader reader) throws IOException {
        List<String> values = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false, inQuotes = false, closed = false, started = false;
        while (true) {
            int next = reader.read();
            if (next == -1) {
                if (inQuotes) throw new IOException("Unclosed CSV quote");
                if (!started) return null;
                values.add(!quoted && field.isEmpty() ? null : field.toString());
                return values;
            }
            char c = (char) next;
            started = true;
            if (inQuotes) {
                if (c == '"') {
                    int peek = reader.read();
                    if (peek == '"') field.append('"');
                    else {
                        inQuotes = false;
                        closed = true;
                        if (peek != -1) reader.unread(peek);
                    }
                } else field.append(c);
            } else if (c == ',' || c == '\r' || c == '\n') {
                values.add(!quoted && field.isEmpty() ? null : field.toString());
                if (c != ',') {
                    if (c == '\r') {
                        int peek = reader.read();
                        if (peek != '\n' && peek != -1) reader.unread(peek);
                    }
                    return values;
                }
                field.setLength(0);
                quoted = false;
                closed = false;
            } else if (c == '"' && field.isEmpty() && !closed) {
                quoted = true;
                inQuotes = true;
            } else {
                if (closed || c == '"') throw new IOException("Illegal character outside CSV quotes");
                field.append(c);
            }
        }
    }
}
