package com.antigravity.billing.service.impl;

import com.antigravity.billing.dto.settings.BusinessSettingsDto;
import com.antigravity.billing.dto.settings.UpdateBusinessSettingsRequest;
import com.antigravity.billing.entity.BusinessSettings;
import com.antigravity.billing.repository.BusinessSettingsRepository;
import com.antigravity.billing.service.AuditService;
import com.antigravity.billing.service.BusinessSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BusinessSettingsServiceImpl implements BusinessSettingsService {

    private final BusinessSettingsRepository settingsRepository;
    private final AuditService auditService;

    @Override
    @Transactional
    public BusinessSettingsDto getSettings() {
        BusinessSettings settings = settingsRepository.findAll().stream().findFirst()
                .orElseGet(() -> settingsRepository.save(BusinessSettings.builder()
                        .legalName("My Enterprise")
                        .tradeName("My Enterprise")
                        .stateName("Maharashtra")
                        .stateCode("27")
                        .invoicePrefix("INV-" + Year.now().getValue() + "-")
                        .invoiceNextSeq(1L)
                        .defaultCurrency("INR")
                        .build()));

        return mapToDto(settings);
    }

    @Override
    @Transactional
    public BusinessSettingsDto updateSettings(UpdateBusinessSettingsRequest request, UUID adminUserId, String adminUsername) {
        BusinessSettings settings = settingsRepository.findAll().stream().findFirst()
                .orElseGet(() -> BusinessSettings.builder().build());

        settings.setLegalName(request.getLegalName());
        settings.setTradeName(request.getTradeName());
        settings.setGstin(request.getGstin());
        settings.setPan(request.getPan());
        settings.setAddressLine1(request.getAddressLine1());
        settings.setAddressLine2(request.getAddressLine2());
        settings.setCity(request.getCity());
        settings.setStateName(request.getStateName());
        settings.setStateCode(request.getStateCode());
        settings.setPinCode(request.getPinCode());
        settings.setPhone(request.getPhone());
        settings.setEmail(request.getEmail());
        settings.setWebsite(request.getWebsite());
        settings.setLogoUrl(request.getLogoUrl());
        settings.setBankName(request.getBankName());
        settings.setBankAccountNumber(request.getBankAccountNumber());
        settings.setBankIfsc(request.getBankIfsc());
        settings.setBankBranch(request.getBankBranch());
        settings.setBankUpiId(request.getBankUpiId());
        settings.setInvoicePrefix(request.getInvoicePrefix());
        settings.setInvoiceTerms(request.getInvoiceTerms());
        settings.setDefaultCurrency(request.getDefaultCurrency());

        BusinessSettings saved = settingsRepository.save(settings);

        auditService.logAction(
                adminUserId,
                adminUsername,
                "UPDATE_SETTINGS",
                "BusinessSettings",
                saved.getId().toString(),
                "Updated business profile and invoice settings"
        );

        return mapToDto(saved);
    }

    @Override
    @Transactional
    public synchronized String generateNextInvoiceNumber() {
        BusinessSettings settings = settingsRepository.findAll().stream().findFirst()
                .orElseGet(() -> settingsRepository.save(BusinessSettings.builder()
                        .legalName("My Enterprise")
                        .stateName("Maharashtra")
                        .stateCode("27")
                        .invoicePrefix("INV-" + Year.now().getValue() + "-")
                        .invoiceNextSeq(1L)
                        .defaultCurrency("INR")
                        .build()));

        Long seq = settings.getInvoiceNextSeq();
        String formattedSeq = String.format("%06d", seq);
        String invoiceNumber = settings.getInvoicePrefix() + formattedSeq;

        settings.setInvoiceNextSeq(seq + 1);
        settingsRepository.save(settings);

        return invoiceNumber;
    }

    private BusinessSettingsDto mapToDto(BusinessSettings entity) {
        return BusinessSettingsDto.builder()
                .id(entity.getId())
                .legalName(entity.getLegalName())
                .tradeName(entity.getTradeName())
                .gstin(entity.getGstin())
                .pan(entity.getPan())
                .addressLine1(entity.getAddressLine1())
                .addressLine2(entity.getAddressLine2())
                .city(entity.getCity())
                .stateName(entity.getStateName())
                .stateCode(entity.getStateCode())
                .pinCode(entity.getPinCode())
                .phone(entity.getPhone())
                .email(entity.getEmail())
                .website(entity.getWebsite())
                .logoUrl(entity.getLogoUrl())
                .bankName(entity.getBankName())
                .bankAccountNumber(entity.getBankAccountNumber())
                .bankIfsc(entity.getBankIfsc())
                .bankBranch(entity.getBankBranch())
                .bankUpiId(entity.getBankUpiId())
                .invoicePrefix(entity.getInvoicePrefix())
                .invoiceNextSeq(entity.getInvoiceNextSeq())
                .invoiceTerms(entity.getInvoiceTerms())
                .defaultCurrency(entity.getDefaultCurrency())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
