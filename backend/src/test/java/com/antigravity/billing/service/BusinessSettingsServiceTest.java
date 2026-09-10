package com.antigravity.billing.service;

import com.antigravity.billing.dto.settings.BusinessSettingsDto;
import com.antigravity.billing.dto.settings.UpdateBusinessSettingsRequest;
import com.antigravity.billing.entity.BusinessSettings;
import com.antigravity.billing.repository.BusinessSettingsRepository;
import com.antigravity.billing.service.impl.BusinessSettingsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BusinessSettingsServiceTest {

    @Mock
    private BusinessSettingsRepository settingsRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private BusinessSettingsServiceImpl settingsService;

    private BusinessSettings sampleSettings;

    @BeforeEach
    void setUp() {
        sampleSettings = BusinessSettings.builder()
                .legalName("Apex Tech Solutions")
                .tradeName("Apex Tech")
                .gstin("27ABCDE1234F1Z5")
                .stateName("Maharashtra")
                .stateCode("27")
                .invoicePrefix("INV-2026-")
                .invoiceNextSeq(42L)
                .defaultCurrency("INR")
                .build();
        sampleSettings.setId(UUID.randomUUID());
    }

    @Test
    @DisplayName("getSettings returns existing settings")
    void testGetSettings() {
        when(settingsRepository.findAll()).thenReturn(List.of(sampleSettings));

        BusinessSettingsDto result = settingsService.getSettings();

        assertThat(result.getLegalName()).isEqualTo("Apex Tech Solutions");
        assertThat(result.getGstin()).isEqualTo("27ABCDE1234F1Z5");
        assertThat(result.getStateCode()).isEqualTo("27");
    }

    @Test
    @DisplayName("generateNextInvoiceNumber formats sequence and increments counter")
    void testGenerateNextInvoiceNumber() {
        when(settingsRepository.findAll()).thenReturn(List.of(sampleSettings));
        when(settingsRepository.save(any(BusinessSettings.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String inv1 = settingsService.generateNextInvoiceNumber();
        assertThat(inv1).isEqualTo("INV-2026-000042");
        assertThat(sampleSettings.getInvoiceNextSeq()).isEqualTo(43L);

        String inv2 = settingsService.generateNextInvoiceNumber();
        assertThat(inv2).isEqualTo("INV-2026-000043");
        assertThat(sampleSettings.getInvoiceNextSeq()).isEqualTo(44L);
    }

    @Test
    @DisplayName("updateSettings persists changes and triggers audit log")
    void testUpdateSettings() {
        when(settingsRepository.findAll()).thenReturn(List.of(sampleSettings));
        when(settingsRepository.save(any(BusinessSettings.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateBusinessSettingsRequest req = new UpdateBusinessSettingsRequest();
        req.setLegalName("Apex International Pvt Ltd");
        req.setTradeName("Apex Global");
        req.setStateName("Gujarat");
        req.setStateCode("24");
        req.setInvoicePrefix("INV-GLOBAL-");
        req.setDefaultCurrency("INR");

        UUID adminId = UUID.randomUUID();
        BusinessSettingsDto updated = settingsService.updateSettings(req, adminId, "admin");

        assertThat(updated.getLegalName()).isEqualTo("Apex International Pvt Ltd");
        assertThat(updated.getStateCode()).isEqualTo("24");
        assertThat(updated.getInvoicePrefix()).isEqualTo("INV-GLOBAL-");

        verify(auditService, times(1)).logAction(eq(adminId), eq("admin"), eq("UPDATE_SETTINGS"), any(), any(), any());
    }
}