package com.shopsphere.controller;

import com.shopsphere.dto.AuthDtos.AuthResponse;
import com.shopsphere.dto.AuthDtos.LoginRequest;
import com.shopsphere.dto.AuthDtos.RegisterRequest;
import com.shopsphere.dto.AuthDtos.UpdateProfileRequest;
import com.shopsphere.dto.AuthDtos.UserResponse;
import com.shopsphere.security.CurrentUser;
import com.shopsphere.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/auth/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/auth/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    /** Logout is client-side (discard the token); tokens are short-lived (JWT_EXPIRATION_MINUTES). */
    @GetMapping("/auth/me")
    public UserResponse me(@AuthenticationPrincipal Jwt jwt) {
        return authService.me(CurrentUser.id(jwt));
    }

    @PutMapping("/users/me")
    public UserResponse updateProfile(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody UpdateProfileRequest request) {
        return authService.updateProfile(CurrentUser.id(jwt), request);
    }
}
