package com.antigravity.billing.controller;

import com.antigravity.billing.dto.common.ApiResponse;
import com.antigravity.billing.dto.settings.BusinessSettingsDto;
import com.antigravity.billing.dto.settings.UpdateBusinessSettingsRequest;
import com.antigravity.billing.security.CustomUserDetails;
import com.antigravity.billing.service.BusinessSettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/settings")
@RequiredArgsConstructor
public class BusinessSettingsController {

    private final BusinessSettingsService settingsService;

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<BusinessSettingsDto>> getProfile() {
        BusinessSettingsDto settings = settingsService.getSettings();
        return ResponseEntity.ok(ApiResponse.ok(settings));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<BusinessSettingsDto>> updateProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody UpdateBusinessSettingsRequest request) {
        BusinessSettingsDto updated = settingsService.updateSettings(
                request,
                userDetails.getId(),
                userDetails.getUsername()
        );
        return ResponseEntity.ok(ApiResponse.ok("Business profile updated successfully", updated));
    }
}
