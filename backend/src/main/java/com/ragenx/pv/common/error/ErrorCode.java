package com.ragenx.pv.common.error;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Registry of error codes. Namespacing is {@code <domain>.<reason>}. Each code maps to
 * one HTTP status. Add codes here as modules need them; never invent codes inline.
 * See docs/conventions.md §4.3.
 */
@Getter
public enum ErrorCode {

    VALIDATION_MISSING_FIELD("validation.missing_field", HttpStatus.BAD_REQUEST),
    VALIDATION_BAD_FORMAT("validation.bad_format", HttpStatus.BAD_REQUEST),
    VALIDATION_INVALID_FIELD_PATH("validation.invalid_field_path", HttpStatus.BAD_REQUEST),
    CASE_NOT_FOUND("case.not_found", HttpStatus.NOT_FOUND),
    CASE_INVALID_FOLLOW_UP("case.invalid_follow_up", HttpStatus.BAD_REQUEST),
    QUERY_CASE_NOT_FOUND("query.case_not_found", HttpStatus.NOT_FOUND),
    RESOURCE_NOT_FOUND("resource.not_found", HttpStatus.NOT_FOUND),
    SYSTEM_UNEXPECTED("system.unexpected", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final HttpStatus httpStatus;

    ErrorCode(String code, HttpStatus httpStatus) {
        this.code = code;
        this.httpStatus = httpStatus;
    }
}
