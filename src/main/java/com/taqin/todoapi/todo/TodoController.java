package com.taqin.todoapi.todo;

import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/todos")
public class TodoController {
    private final TodoService todoService;

    public TodoController(TodoService todoService) {
        this.todoService = todoService;
    }

    @GetMapping
    public PageResponse<TodoResponse> findAll(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestParam(required = false) Boolean completed,
            @RequestParam(required = false) String q
    ) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);
        Sort.Direction sortDirection = "asc".equalsIgnoreCase(direction)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        PageRequest pageRequest = PageRequest.of(safePage, safeSize, Sort.by(sortDirection, safeSortBy(sortBy)));

        return PageResponse.from(todoService.findAll(jwt.getSubject(), completed, q, pageRequest), TodoResponse::from);
    }

    private String safeSortBy(String sortBy) {
        return switch (sortBy) {
            case "id", "title", "completed", "createdAt", "dueAt" -> sortBy;
            default -> "createdAt";
        };
    }

    @GetMapping("/{id}")
    public TodoResponse findById(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return TodoResponse.from(todoService.findById(id, jwt.getSubject()));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TodoResponse create(@Valid @RequestBody TodoRequest request, @AuthenticationPrincipal Jwt jwt) {
        return TodoResponse.from(todoService.create(request, jwt.getSubject()));
    }

    @PutMapping("/{id}")
    public TodoResponse update(
            @PathVariable Long id,
            @Valid @RequestBody TodoRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return TodoResponse.from(todoService.update(id, request, jwt.getSubject()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        todoService.delete(id, jwt.getSubject());
    }
}
