package com.antigravity.billing.dto.supplier;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplierResponseDto {
    private UUID id;
    private String name;
    private String businessName;
    private String phone;
    private String email;
    private String address;
    private String city;
    private String stateName;
    private String stateCode;
    private String pinCode;
    private String gstin;
    private BigDecimal outstandingBalance;
    private String notes;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;
}