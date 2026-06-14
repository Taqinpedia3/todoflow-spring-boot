package com.taqin.todoapi.auth;

public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException() {
        super("Email atau password salah");
    }
}
