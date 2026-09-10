package com.antigravity.billing.dto.settings;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessSettingsDto {
    private UUID id;
    private String legalName;
    private String tradeName;
    private String gstin;
    private String pan;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String stateName;
    private String stateCode;
    private String pinCode;
    private String phone;
    private String email;
    private String website;
    private String logoUrl;
    private String bankName;
    private String bankAccountNumber;
    private String bankIfsc;
    private String bankBranch;
    private String bankUpiId;
    private String invoicePrefix;
    private Long invoiceNextSeq;
    private String invoiceTerms;
    private String defaultCurrency;
    private Instant updatedAt;
}
