package com.ragenx.pv.common.trace;

import com.ragenx.pv.common.constants.Constants;
import com.ragenx.pv.common.util.TraceContext;
import com.ragenx.pv.common.util.TraceIdGenerator;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Edge filter: every inbound request gets a trace id (the caller's X-Trace-Id if
 * present, otherwise a freshly generated one). The id is placed in the MDC so every
 * log line for the request carries it, and echoed back on the response header.
 * On completion it logs one access-style line (method, path, status, latency) so every
 * request — not just errors — is visible in the logs with its trace id. MDC is always
 * cleared in a finally block. See docs/conventions.md §5.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        String incoming = request.getHeader(Constants.TRACE_ID_HEADER);
        String traceId = StringUtils.hasText(incoming) ? incoming : TraceIdGenerator.generate();
        TraceContext.setTraceId(traceId);
        response.setHeader(Constants.TRACE_ID_HEADER, traceId);
        long startMs = System.currentTimeMillis();
        try {
            chain.doFilter(request, response);
        } finally {
            // Logged while the trace id is still in the MDC (the Logback pattern prints it).
            log.info("request.completed method={} path={} status={} latency_ms={}",
                    request.getMethod(), request.getRequestURI(), response.getStatus(),
                    System.currentTimeMillis() - startMs);
            TraceContext.clear();
        }
    }
}
