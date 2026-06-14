package com.taqin.todoapi.auth;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            AppUserRepository appUserRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = request.email().toLowerCase();
        if (appUserRepository.existsByEmail(email)) {
            throw new EmailAlreadyRegisteredException(email);
        }

        String passwordHash = passwordEncoder.encode(request.password());
        AppUser user = new AppUser(request.name(), email, passwordHash);

        return UserResponse.from(appUserRepository.save(user));
    }

    public LoginResponse login(LoginRequest request) {
        String email = request.email().toLowerCase();
        AppUser user = appUserRepository.findByEmail(email)
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        return new LoginResponse(
                "Login berhasil",
                "Bearer",
                jwtService.generateToken(user),
                UserResponse.from(user)
        );
    }
}
