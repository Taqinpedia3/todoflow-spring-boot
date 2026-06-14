package com.taqin.todoapi.auth;

public class EmailAlreadyRegisteredException extends RuntimeException {
    public EmailAlreadyRegisteredException(String email) {
        super("Email " + email + " sudah terdaftar");
    }
}
