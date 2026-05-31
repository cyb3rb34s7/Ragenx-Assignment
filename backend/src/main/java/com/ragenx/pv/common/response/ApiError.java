package com.ragenx.pv.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Value;

/**
 * Error sub-object of the response envelope. UI logic branches on {@code code},
 * never on {@code message}. See docs/conventions.md §4.2.
 */
@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiError {

    String code;
    String message;
    Object details;
}
