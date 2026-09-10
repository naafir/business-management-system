package com.antigravity.billing.service;

import com.antigravity.billing.dto.settings.BusinessSettingsDto;
import com.antigravity.billing.dto.settings.UpdateBusinessSettingsRequest;

import java.util.UUID;

public interface BusinessSettingsService {
    BusinessSettingsDto getSettings();
    BusinessSettingsDto updateSettings(UpdateBusinessSettingsRequest request, UUID adminUserId, String adminUsername);
    String generateNextInvoiceNumber();
}
