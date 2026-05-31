package com.ragenx.pv.common.error;

import lombok.Getter;

/**
 * Domain exception. Carries an {@link ErrorCode} (which fixes the HTTP status) plus an
 * optional structured {@code details} payload. The global handler renders it into the
 * standard error envelope. See docs/conventions.md §7.
 */
@Getter
public class ApiException extends RuntimeException {

    private final ErrorCode errorCode;
    private final transient Object details;

    public ApiException(ErrorCode errorCode, String message) {
        this(errorCode, message, null);
    }

    public ApiException(ErrorCode errorCode, String message, Object details) {
        super(message);
        this.errorCode = errorCode;
        this.details = details;
    }
}
