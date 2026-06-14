package com.taqin.todoapi.todo;

import java.time.Instant;

public record TodoResponse(
        Long id,
        String title,
        boolean completed,
        Instant createdAt,
        Instant dueAt
) {
    public static TodoResponse from(Todo todo) {
        return new TodoResponse(
                todo.getId(),
                todo.getTitle(),
                todo.isCompleted(),
                todo.getCreatedAt(),
                todo.getDueAt()
        );
    }
}
