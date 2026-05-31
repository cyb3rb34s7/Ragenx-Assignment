package com.ragenx.pv.common.response;

import com.ragenx.pv.common.error.ErrorCode;
import com.ragenx.pv.common.util.TraceContext;

/**
 * Builds the standard response envelope, stamping the current request's trace id from
 * the MDC. Controllers use {@link #ok}; the global exception handler uses {@link #error}.
 */
public final class ResponseFactory {

    private ResponseFactory() {
    }

    public static <T> ApiResponse<T> ok(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .traceId(TraceContext.getTraceId())
                .build();
    }

    public static ApiResponse<Object> error(ErrorCode code, String message, Object details) {
        return ApiResponse.builder()
                .success(false)
                .error(ApiError.builder()
                        .code(code.getCode())
                        .message(message)
                        .details(details)
                        .build())
                .traceId(TraceContext.getTraceId())
                .build();
    }
}
