package io.asteria.infrastructure.trace;

import io.asteria.common.trace.TraceConstants;
import io.asteria.common.trace.TraceIdGenerator;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;

import java.nio.charset.StandardCharsets;

/**
 * Reads correlation metadata without changing Kafka payloads.
 */
public final class KafkaTraceUtils {
    private KafkaTraceUtils() {
    }

    public static String resolveTraceId(Headers headers) {
        Header header = headers == null ? null : headers.lastHeader(TraceConstants.TRACE_ID_HEADER);
        String traceId = header == null || header.value() == null ? null
                : new String(header.value(), StandardCharsets.UTF_8);
        return TraceIdGenerator.resolve(traceId);
    }
}
