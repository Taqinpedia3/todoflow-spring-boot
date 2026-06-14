package com.taqin.todoapi.todo;

import java.time.Instant;
import java.util.Map;

public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        Map<String, String> errors
) {
    public static ApiErrorResponse of(int status, String error, String message) {
        return new ApiErrorResponse(Instant.now(), status, error, message, Map.of());
    }

    public static ApiErrorResponse withErrors(
            int status,
            String error,
            String message,
            Map<String, String> errors
    ) {
        return new ApiErrorResponse(Instant.now(), status, error, message, errors);
    }
}
