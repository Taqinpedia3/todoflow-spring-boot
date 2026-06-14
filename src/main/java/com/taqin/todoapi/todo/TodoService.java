package com.taqin.todoapi.todo;

import com.taqin.todoapi.auth.AppUser;
import com.taqin.todoapi.auth.AppUserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class TodoService {
    private final TodoRepository todoRepository;
    private final AppUserRepository appUserRepository;

    public TodoService(TodoRepository todoRepository, AppUserRepository appUserRepository) {
        this.todoRepository = todoRepository;
        this.appUserRepository = appUserRepository;
    }

    public Page<Todo> findAll(String ownerEmail, Boolean completed, String q, Pageable pageable) {
        String normalizedQuery = q == null || q.isBlank() ? null : q.trim();
        return todoRepository.searchByOwnerEmail(ownerEmail, completed, normalizedQuery, pageable);
    }

    public Todo findById(Long id, String ownerEmail) {
        return todoRepository.findByIdAndOwnerEmail(id, ownerEmail)
                .orElseThrow(() -> new TodoNotFoundException(id));
    }

    public Todo create(TodoRequest request, String ownerEmail) {
        AppUser owner = appUserRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new TodoOwnerNotFoundException(ownerEmail));
        Todo todo = new Todo(request.title(), owner);
        if (request.completed() != null) {
            todo.setCompleted(request.completed());
        }
        todo.setDueAt(request.dueAt());
        return todoRepository.save(todo);
    }

    public Todo update(Long id, TodoRequest request, String ownerEmail) {
        Todo todo = findById(id, ownerEmail);
        todo.setTitle(request.title());
        if (request.completed() != null) {
            todo.setCompleted(request.completed());
        }
        todo.setDueAt(request.dueAt());
        return todoRepository.save(todo);
    }

    public void delete(Long id, String ownerEmail) {
        Todo todo = findById(id, ownerEmail);
        todoRepository.delete(todo);
    }
}
