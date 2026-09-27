package com.onlineassessment.controller;

import com.onlineassessment.dto.ApiResponse;
import com.onlineassessment.dto.AuthDtos.ChangePasswordRequest;
import com.onlineassessment.dto.AuthDtos.LoginRequest;
import com.onlineassessment.dto.AuthDtos.RegisterRequest;
import com.onlineassessment.dto.AuthDtos.UpdateProfileRequest;
import com.onlineassessment.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService auth;

    @PostMapping("/register")
    public ApiResponse<?> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.ok("Registered successfully", auth.register(request));
    }

    @PostMapping("/login")
    public ApiResponse<?> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok("Login successful", auth.login(request));
    }

    @GetMapping("/me")
    public ApiResponse<?> me(Authentication authentication) {
        return ApiResponse.ok("Profile", auth.profile(authentication.getName()));
    }

    @PutMapping("/me")
    public ApiResponse<?> updateProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ApiResponse.ok(
                "Profile updated",
                auth.updateProfile(authentication.getName(), request)
        );
    }

    @PutMapping("/change-password")
    public ApiResponse<?> changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request) {
        auth.change(authentication.getName(), request);
        return ApiResponse.ok("Password changed");
    }

    @PostMapping("/logout")
    public ApiResponse<?> logout() {
        return ApiResponse.ok("Logged out. Remove the JWT from the client.");
    }
}
