package com.antigravity.billing.dto.customer;

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
public class CustomerResponseDto {
    private UUID id;
    private String name;
    private String businessName;
    private String phone;
    private String email;
    private String billingAddress;
    private String shippingAddress;
    private String city;
    private String stateName;
    private String stateCode;
    private String pinCode;
    private String gstin;
    private String customerType;
    private BigDecimal creditLimit;
    private BigDecimal outstandingBalance;
    private String notes;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;
}