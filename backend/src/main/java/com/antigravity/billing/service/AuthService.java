package com.antigravity.billing.service;

import com.antigravity.billing.dto.auth.ChangePasswordRequest;
import com.antigravity.billing.dto.auth.LoginRequest;
import com.antigravity.billing.dto.auth.LoginResponse;
import com.antigravity.billing.dto.auth.UserSummaryDto;

import java.util.UUID;

public interface AuthService {
    LoginResponse login(LoginRequest request);
    UserSummaryDto getCurrentUser(UUID userId);
    void changePassword(UUID userId, ChangePasswordRequest request);
}
