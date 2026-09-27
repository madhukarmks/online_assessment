package com.onlineassessment.service;

import com.onlineassessment.dto.AuthDtos.AuthResponse;
import com.onlineassessment.dto.AuthDtos.ChangePasswordRequest;
import com.onlineassessment.dto.AuthDtos.LoginRequest;
import com.onlineassessment.dto.AuthDtos.ProfileResponse;
import com.onlineassessment.dto.AuthDtos.RegisterRequest;
import com.onlineassessment.dto.AuthDtos.UpdateProfileRequest;
import com.onlineassessment.entity.Role;
import com.onlineassessment.entity.User;
import com.onlineassessment.exception.Exceptions.BadRequest;
import com.onlineassessment.exception.Exceptions.NotFound;
import com.onlineassessment.repository.UserRepository;
import com.onlineassessment.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());

        if (users.findByEmail(email).isPresent()) {
            throw new BadRequest("Email already registered");
        }

        User user = new User();
        user.setName(request.name().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(Role.STUDENT);
        user.setActive(true);

        users.save(user);
        return auth(user);
    }

    public AuthResponse login(LoginRequest request) {
        User user = users.findByEmail(normalizeEmail(request.email()))
                .orElseThrow(() -> new BadRequest("Invalid credentials"));

        if (!user.isActive() || !passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BadRequest("Invalid credentials");
        }

        return auth(user);
    }

    private AuthResponse auth(User user) {
        return new AuthResponse(
                jwtService.generate(user.getId(), user.getEmail(), user.getRole().name()),
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole().name()
        );
    }

    public ProfileResponse profile(String email) {
        User user = user(email);
        return profileResponse(user);
    }

    @Transactional
    public ProfileResponse updateProfile(String email, UpdateProfileRequest request) {
        User user = user(email);
        user.setName(request.name().trim());
        users.save(user);
        return profileResponse(user);
    }

    @Transactional
    public void change(String email, ChangePasswordRequest request) {
        User user = user(email);

        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new BadRequest("Current password is incorrect");
        }

        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            throw new BadRequest("New password must be different from current password");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        users.save(user);
    }

    public User user(String email) {
        return users.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new NotFound("User not found"));
    }

    private ProfileResponse profileResponse(User user) {
        return new ProfileResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole().name(),
                user.isActive()
        );
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
