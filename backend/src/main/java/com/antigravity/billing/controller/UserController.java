package com.antigravity.billing.controller;

import com.antigravity.billing.dto.auth.CreateUserRequest;
import com.antigravity.billing.dto.auth.UserDetailDto;
import com.antigravity.billing.dto.common.ApiResponse;
import com.antigravity.billing.entity.User;
import com.antigravity.billing.exception.ApiException;
import com.antigravity.billing.exception.ResourceNotFoundException;
import com.antigravity.billing.repository.UserRepository;
import com.antigravity.billing.service.AuditService;
import com.antigravity.billing.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * User Management API — accessible to ROLE_OWNER only.
 * Allows creating, listing, and toggling staff user accounts.
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('OWNER')")
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    /** List all users in the system */
    @GetMapping
    public ResponseEntity<ApiResponse<List<UserDetailDto>>> listUsers() {
        List<UserDetailDto> users = userRepository.findAll().stream()
                .map(this::toDto)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(users));
    }

    /** Create a new staff user */
    @PostMapping
    public ResponseEntity<ApiResponse<UserDetailDto>> createUser(
            @Valid @RequestBody CreateUserRequest request,
            @AuthenticationPrincipal CustomUserDetails creator) {

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ApiException("Username '" + request.getUsername() + "' is already taken", HttpStatus.CONFLICT);
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ApiException("Email '" + request.getEmail() + "' is already registered", HttpStatus.CONFLICT);
        }

        User newUser = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .fullName(request.getFullName())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .active(true)
                .build();

        User saved = userRepository.save(newUser);

        auditService.logAction(
                creator.getId(), creator.getUsername(),
                "CREATE_USER", "User", saved.getId().toString(),
                "Created user: " + saved.getUsername() + " (" + saved.getRole() + ")"
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("User created successfully", toDto(saved)));
    }

    /** Enable or disable a user account */
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<UserDetailDto>> toggleUserStatus(
            @PathVariable UUID id,
            @RequestParam boolean active,
            @AuthenticationPrincipal CustomUserDetails actor) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        // Prevent owner from disabling themselves
        if (actor.getId().equals(id) && !active) {
            throw new ApiException("You cannot disable your own account", HttpStatus.BAD_REQUEST);
        }

        user.setActive(active);
        userRepository.save(user);

        auditService.logAction(
                actor.getId(), actor.getUsername(),
                active ? "ENABLE_USER" : "DISABLE_USER",
                "User", id.toString(),
                (active ? "Enabled" : "Disabled") + " user: " + user.getUsername()
        );

        return ResponseEntity.ok(ApiResponse.ok(
                "User " + (active ? "enabled" : "disabled") + " successfully", toDto(user)));
    }

    /** Reset a user's password */
    @PatchMapping("/{id}/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @PathVariable UUID id,
            @RequestParam String newPassword,
            @AuthenticationPrincipal CustomUserDetails actor) {

        if (newPassword.length() < 8) {
            throw new ApiException("Password must be at least 8 characters", HttpStatus.BAD_REQUEST);
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        auditService.logAction(
                actor.getId(), actor.getUsername(),
                "RESET_PASSWORD", "User", id.toString(),
                "Password reset for user: " + user.getUsername()
        );

        return ResponseEntity.ok(ApiResponse.ok("Password reset successfully", null));
    }

    private UserDetailDto toDto(User user) {
        return UserDetailDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .active(user.isActive())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
