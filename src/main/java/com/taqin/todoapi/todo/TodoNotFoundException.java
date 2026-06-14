package com.taqin.todoapi.todo;

public class TodoNotFoundException extends RuntimeException {
    public TodoNotFoundException(Long id) {
        super("Todo dengan id " + id + " tidak ditemukan");
    }
}
