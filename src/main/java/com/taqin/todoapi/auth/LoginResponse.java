package com.taqin.todoapi.auth;

public record LoginResponse(
        String message,
        String tokenType,
        String accessToken,
        UserResponse user
) {
}
