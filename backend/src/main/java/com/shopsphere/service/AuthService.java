package com.shopsphere.service;

import com.shopsphere.dto.AuthDtos.AuthResponse;
import com.shopsphere.dto.AuthDtos.LoginRequest;
import com.shopsphere.dto.AuthDtos.RegisterRequest;
import com.shopsphere.dto.AuthDtos.UpdateProfileRequest;
import com.shopsphere.dto.AuthDtos.UserResponse;
import com.shopsphere.entity.RoleName;
import com.shopsphere.entity.User;
import com.shopsphere.exception.AuthenticationFailedException;
import com.shopsphere.exception.BusinessException;
import com.shopsphere.exception.NotFoundException;
import com.shopsphere.repository.RoleRepository;
import com.shopsphere.repository.UserRepository;
import com.shopsphere.security.JwtTokenService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class AuthService {

    private final UserRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder encoder;
    private final JwtTokenService tokens;

    public AuthService(UserRepository users, RoleRepository roles, PasswordEncoder encoder, JwtTokenService tokens) {
        this.users = users;
        this.roles = roles;
        this.encoder = encoder;
        this.tokens = tokens;
    }

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        String email = req.email().trim().toLowerCase(Locale.ROOT);
        if (users.existsByEmailIgnoreCase(email)) {
            throw new BusinessException("An account with this email already exists");
        }
        User user = new User(email, encoder.encode(req.password()), req.fullName().trim());
        user.getRoles().add(roles.findByName(RoleName.CUSTOMER)
                .orElseThrow(() -> new IllegalStateException("CUSTOMER role missing - check Flyway migrations")));
        users.save(user);
        var token = tokens.issue(user);
        return new AuthResponse(token.value(), token.expiresAt(), Mappers.toDto(user));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        User user = users.findByEmailIgnoreCase(req.email().trim())
                .orElseThrow(AuthenticationFailedException::new);
        if (!encoder.matches(req.password(), user.getPasswordHash())) {
            throw new AuthenticationFailedException();
        }
        var token = tokens.issue(user);
        return new AuthResponse(token.value(), token.expiresAt(), Mappers.toDto(user));
    }

    @Transactional(readOnly = true)
    public UserResponse me(Long userId) {
        return Mappers.toDto(find(userId));
    }

    @Transactional
    public UserResponse updateProfile(Long userId, UpdateProfileRequest req) {
        User user = find(userId);
        user.setFullName(req.fullName().trim());
        user.setPhone(req.phone() == null || req.phone().isBlank() ? null : req.phone().trim());
        return Mappers.toDto(user);
    }

    private User find(Long userId) {
        return users.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
    }
}
