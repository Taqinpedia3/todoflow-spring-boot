package com.taqin.todoapi.todo;

import com.taqin.todoapi.auth.AppUser;
import com.taqin.todoapi.auth.AppUserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.seed-demo", havingValue = "true")
public class DataSeeder implements CommandLineRunner {
    private final TodoRepository todoRepository;
    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(
            TodoRepository todoRepository,
            AppUserRepository appUserRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.todoRepository = todoRepository;
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (todoRepository.count() == 0) {
            AppUser demoUser = appUserRepository.findByEmail("demo@example.com")
                    .orElseGet(() -> appUserRepository.save(new AppUser(
                            "Demo User",
                            "demo@example.com",
                            passwordEncoder.encode("password123")
                    )));
            todoRepository.save(new Todo("Belajar Spring Boot", demoUser));
            todoRepository.save(new Todo("Membuat REST API Todo", demoUser));
        }
    }
}
