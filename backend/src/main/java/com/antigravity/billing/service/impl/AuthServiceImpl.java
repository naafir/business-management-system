package com.antigravity.billing.service.impl;

import com.antigravity.billing.dto.auth.ChangePasswordRequest;
import com.antigravity.billing.dto.auth.LoginRequest;
import com.antigravity.billing.dto.auth.LoginResponse;
import com.antigravity.billing.dto.auth.UserSummaryDto;
import com.antigravity.billing.entity.User;
import com.antigravity.billing.exception.ApiException;
import com.antigravity.billing.exception.ResourceNotFoundException;
import com.antigravity.billing.repository.UserRepository;
import com.antigravity.billing.security.CustomUserDetails;
import com.antigravity.billing.security.JwtTokenProvider;
import com.antigravity.billing.service.AuditService;
import com.antigravity.billing.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    @Override
    public LoginResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        String token = tokenProvider.generateToken(authentication);
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        auditService.logAction(
                userDetails.getId(),
                userDetails.getUsername(),
                "LOGIN_SUCCESS",
                "User",
                userDetails.getId().toString(),
                "User logged in successfully"
        );

        UserSummaryDto userSummary = UserSummaryDto.builder()
                .id(userDetails.getId())
                .username(userDetails.getUsername())
                .email(userDetails.getEmail())
                .fullName(userDetails.getFullName())
                .role(userDetails.getAuthorities().stream().findFirst().map(Object::toString).orElse("ROLE_ADMIN"))
                .build();

        return LoginResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresInMs(tokenProvider.getExpirationMs())
                .user(userSummary)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public UserSummaryDto getCurrentUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        return UserSummaryDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .build();
    }

    @Override
    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new ApiException("New password and confirm password do not match", HttpStatus.BAD_REQUEST);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new ApiException("Current password does not match", HttpStatus.BAD_REQUEST);
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        auditService.logAction(
                user.getId(),
                user.getUsername(),
                "CHANGE_PASSWORD",
                "User",
                user.getId().toString(),
                "Password changed successfully"
        );
    }
}
