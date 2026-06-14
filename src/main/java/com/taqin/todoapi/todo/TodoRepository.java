package com.taqin.todoapi.todo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface TodoRepository extends JpaRepository<Todo, Long> {
    @Query("""
            select todo from Todo todo
            where todo.owner.email = :email
              and (:completed is null or todo.completed = :completed)
              and (:q is null or lower(todo.title) like lower(concat('%', :q, '%')))
            """)
    Page<Todo> searchByOwnerEmail(String email, Boolean completed, String q, Pageable pageable);

    Optional<Todo> findByIdAndOwnerEmail(Long id, String email);
}
