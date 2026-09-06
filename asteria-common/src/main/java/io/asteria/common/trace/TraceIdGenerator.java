package io.asteria.common.trace;

import org.apache.commons.lang3.StringUtils;

import java.util.UUID;

/**
 * Generates lightweight correlation IDs without business ID infrastructure.
 */
public final class TraceIdGenerator {
    private TraceIdGenerator() {
    }

    public static String generate() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * Keeps supplied IDs or generates one when absent.
     */
    public static String resolve(String traceId) {
        return StringUtils.isBlank(traceId) ? generate() : traceId;
    }
}
