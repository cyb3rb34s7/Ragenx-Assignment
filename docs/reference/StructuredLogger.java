package com.cms_monitoring_system.common.util;

import lombok.extern.slf4j.Slf4j; import org.slf4j.MDC;

@Slf4j public class StructuredLogger {

private static final String TRACE_ID = "traceId";
private static final String OPERATION = "operation";
private static final String SOURCE_SERVICE = "sourceService";
private static final String ENVIRONMENT = "environment";
private static final String REGION = "region";
private static final String ERROR_TYPE = "errorType";
private static final String ERROR_MESSAGE = "errorMessage";
private static final String SUCCESS = "success";
private static final String DURATION = "duration";
private static final String RETRY_ATTEMPT = "retryAttempt";
private static final String MUTE_REASON = "muteReason";

private StructuredLogger() {}

public static void setTraceId(String traceId) {
    MDC.put(TRACE_ID, traceId);
}

public static void setRequestContext(String operation, String sourceService, 
                                   String environment, String region, String errorType,String errorMessage) {
    MDC.put(OPERATION, operation);
    MDC.put(SOURCE_SERVICE, sourceService);
    MDC.put(ENVIRONMENT, environment);
    MDC.put(REGION, region);
    MDC.put(ERROR_TYPE, errorType);
    MDC.put(ERROR_MESSAGE,errorMessage);
}

public static void setSuccess(boolean success) {
    MDC.put(SUCCESS, String.valueOf(success));
}

public static void setDuration(long duration) {
    MDC.put(DURATION, String.valueOf(duration));
}

public static void setRetryAttempt(int attempt) {
    MDC.put(RETRY_ATTEMPT, String.valueOf(attempt));
}

public static void clearRetryAttempt() {
    MDC.remove(RETRY_ATTEMPT);
}

public static void clearContext() {
    MDC.clear();
}

public static void logRequestReceived(String message) {
    log.info("REQUEST_RECEIVED: {}", message);
}

public static void logOperationStart(String message) {
    log.info("OPERATION_START: {}", message);
}

public static void logOperationSuccess(String message) {
    setSuccess(true);
    log.info("OPERATION_SUCCESS: {}", message);
}

public static void logOperationFailure(String message, Exception ex) {
    setSuccess(false);
    log.error("OPERATION_FAILURE: {}", message, ex);
}

public static void logRetryAttempt(String message, int attempt) {
    setRetryAttempt(attempt);
    log.warn("RETRY_ATTEMPT: {} (attempt: {})", message, attempt);
}

public static void logMutedRequest(String reason) {
    MDC.put(MUTE_REASON, reason);
    log.info("REQUEST_MUTED: Error request muted due to {}", reason);
    MDC.remove(MUTE_REASON);
}

public static String getTraceId() {
    return MDC.get(TRACE_ID);
}
}
