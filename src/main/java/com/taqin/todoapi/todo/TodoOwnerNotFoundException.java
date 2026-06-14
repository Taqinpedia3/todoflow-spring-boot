package com.taqin.todoapi.todo;

public class TodoOwnerNotFoundException extends RuntimeException {
    public TodoOwnerNotFoundException(String email) {
        super("User " + email + " tidak ditemukan");
    }
}
