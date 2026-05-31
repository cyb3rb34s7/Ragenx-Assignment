package com.ragenx.pv.common.util;

import com.ragenx.pv.common.constants.Constants;
import org.slf4j.MDC;

/**
 * Slim wrapper around SLF4J MDC for the request trace id. This is the trimmed
 * replacement for the prior project's StructuredLogger, whose extra fields
 * (operation, region, retryAttempt, ...) were specific to that system. Here the
 * only request-scoped log field we need is the trace id. See docs/conventions.md §5.3.
 */
public final class TraceContext {

    private TraceContext() {
    }

    public static void setTraceId(String traceId) {
        MDC.put(Constants.TRACE_ID_MDC_KEY, traceId);
    }

    public static String getTraceId() {
        return MDC.get(Constants.TRACE_ID_MDC_KEY);
    }

    public static void clear() {
        MDC.remove(Constants.TRACE_ID_MDC_KEY);
    }
}
