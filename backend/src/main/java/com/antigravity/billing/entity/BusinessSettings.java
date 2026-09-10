package com.antigravity.billing.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "business_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BusinessSettings extends BaseEntity {

    @Column(name = "legal_name", nullable = false, length = 200)
    private String legalName;

    @Column(name = "trade_name", length = 200)
    private String tradeName;

    @Column(name = "gstin", length = 15)
    private String gstin;

    @Column(name = "pan", length = 10)
    private String pan;

    @Column(name = "address_line1")
    private String addressLine1;

    @Column(name = "address_line2")
    private String addressLine2;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "state_name", nullable = false, length = 100)
    private String stateName;

    @Column(name = "state_code", nullable = false, length = 2)
    private String stateCode;

    @Column(name = "pin_code", length = 10)
    private String pinCode;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "website", length = 150)
    private String website;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(name = "bank_name", length = 100)
    private String bankName;

    @Column(name = "bank_account_number", length = 50)
    private String bankAccountNumber;

    @Column(name = "bank_ifsc", length = 20)
    private String bankIfsc;

    @Column(name = "bank_branch", length = 100)
    private String bankBranch;

    @Column(name = "bank_upi_id", length = 100)
    private String bankUpiId;

    @Column(name = "invoice_prefix", nullable = false, length = 20)
    @Builder.Default
    private String invoicePrefix = "INV-";

    @Column(name = "invoice_next_seq", nullable = false)
    @Builder.Default
    private Long invoiceNextSeq = 1L;

    @Column(name = "invoice_terms", columnDefinition = "TEXT")
    private String invoiceTerms;

    @Column(name = "default_currency", nullable = false, length = 3)
    @Builder.Default
    private String defaultCurrency = "INR";
}
