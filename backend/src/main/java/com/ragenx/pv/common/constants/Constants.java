package com.ragenx.pv.common.constants;

/**
 * Cross-cutting constants for the common layer. No magic strings in critical logic
 * (see docs/conventions.md §6).
 */
public final class Constants {

    private Constants() {
    }

    /** SLF4J MDC key under which the per-request trace id is stored. */
    public static final String TRACE_ID_MDC_KEY = "traceId";

    /** HTTP header used to receive/propagate the trace id. */
    public static final String TRACE_ID_HEADER = "X-Trace-Id";
}
