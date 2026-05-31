package com.ragenx.pv.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Value;

/**
 * The single response envelope used by every endpoint, success or error, so the
 * frontend branches once on {@code success}. {@code trace_id} is always present.
 * See docs/conventions.md §4.
 *
 * @param <T> the success payload type
 */
@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    boolean success;
    T data;
    ApiError error;
    String traceId;
}
