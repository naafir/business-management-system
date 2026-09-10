package com.antigravity.billing.controller;

import com.antigravity.billing.dto.auth.ChangePasswordRequest;
import com.antigravity.billing.dto.auth.LoginRequest;
import com.antigravity.billing.dto.auth.LoginResponse;
import com.antigravity.billing.dto.auth.UserSummaryDto;
import com.antigravity.billing.dto.common.ApiResponse;
import com.antigravity.billing.security.CustomUserDetails;
import com.antigravity.billing.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.ok("Login successful", response));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserSummaryDto>> getCurrentUser(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        UserSummaryDto user = authService.getCurrentUser(userDetails.getId());
        return ResponseEntity.ok(ApiResponse.ok(user));
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(userDetails.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok("Password changed successfully", null));
    }
}
