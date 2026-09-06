package io.asteria.common.trace;

/**
 * Shared trace names independent of transport implementations.
 */
public final class TraceConstants {
    public static final String TRACE_ID = "traceId";
    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    private TraceConstants() {
    }
}
