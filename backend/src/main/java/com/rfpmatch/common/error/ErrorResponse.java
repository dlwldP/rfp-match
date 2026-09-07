package com.rfpmatch.common.error;

import java.time.OffsetDateTime;
import org.springframework.http.HttpStatus;

/** 전 API 공통 에러 응답 포맷. */
public record ErrorResponse(
        OffsetDateTime timestamp,
        int status,
        String error,
        String message,
        String path) {

    public static ErrorResponse of(HttpStatus status, String message, String path) {
        return new ErrorResponse(OffsetDateTime.now(), status.value(), status.name(), message, path);
    }
}
