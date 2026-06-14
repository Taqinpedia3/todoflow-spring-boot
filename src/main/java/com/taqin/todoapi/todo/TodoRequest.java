package com.taqin.todoapi.todo;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

public record TodoRequest(
        @NotBlank(message = "Title wajib diisi")
        String title,
        Boolean completed,
        Instant dueAt
) {
}
