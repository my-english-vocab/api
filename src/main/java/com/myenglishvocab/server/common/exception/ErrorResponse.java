package com.myenglishvocab.server.common.exception;

import java.time.LocalDateTime;

public record ErrorResponse(
        String code,
        String message,
        LocalDateTime timestamp,
        String path
) {
    public static ErrorResponse of(ErrorCode errorCode, String path) {
        return new ErrorResponse(
                errorCode.getCode(),
                errorCode.getMessage(),
                LocalDateTime.now(),
                path
        );
    }

    public static ErrorResponse of(ErrorCode errorCode, String path, String message) {
        return new ErrorResponse(
                errorCode.getCode(),
                message,
                LocalDateTime.now(),
                path
        );
    }
}
