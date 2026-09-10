package com.antigravity.billing.dto.settings;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateBusinessSettingsRequest {
    @NotBlank(message = "Legal name is required")
    @Size(max = 200, message = "Legal name must not exceed 200 characters")
    private String legalName;

    @Size(max = 200, message = "Trade name must not exceed 200 characters")
    private String tradeName;

    @Pattern(regexp = "^$|^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$", message = "Invalid GSTIN format")
    private String gstin;

    @Pattern(regexp = "^$|^[A-Z]{5}[0-9]{4}[A-Z]{1}$", message = "Invalid PAN format")
    private String pan;

    private String addressLine1;
    private String addressLine2;
    private String city;

    @NotBlank(message = "State name is required")
    private String stateName;

    @NotBlank(message = "State code is required")
    @Pattern(regexp = "^[0-9]{2}$", message = "State code must be 2 digits (e.g., 27 for Maharashtra)")
    private String stateCode;

    @Pattern(regexp = "^$|^[1-9][0-9]{5}$", message = "PIN code must be 6 digits")
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

    @NotBlank(message = "Invoice prefix is required")
    private String invoicePrefix;

    private String invoiceTerms;

    @NotBlank(message = "Default currency is required")
    @Size(min = 3, max = 3, message = "Currency must be 3-letter ISO code (e.g. INR)")
    private String defaultCurrency;
}
