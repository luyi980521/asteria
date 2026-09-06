package io.asteria.common.trace;

import org.slf4j.MDC;

/**
 * Temporarily binds an event trace, preserving nested job or callback thread context.
 */
public final class TraceScope implements AutoCloseable {
    private final String previous;

    private TraceScope(String traceId) {
        previous = MDC.get(TraceConstants.TRACE_ID);
        MDC.put(TraceConstants.TRACE_ID, traceId);
    }

    public static TraceScope open(String traceId) {
        return new TraceScope(traceId);
    }

    @Override
    public void close() {
        if (previous == null) {
            MDC.remove(TraceConstants.TRACE_ID);
        } else {
            MDC.put(TraceConstants.TRACE_ID, previous);
        }
    }
}
