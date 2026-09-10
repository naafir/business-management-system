package com.antigravity.billing.dto.customer;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateCustomerRequest {
    @NotBlank(message = "Customer name is required")
    @Size(max = 200, message = "Customer name must not exceed 200 characters")
    private String name;

    @Size(max = 200, message = "Business name must not exceed 200 characters")
    private String businessName;

    private String phone;
    private String email;
    private String billingAddress;
    private String shippingAddress;
    private String city;

    @NotBlank(message = "State name is required")
    private String stateName;

    @NotBlank(message = "State code is required")
    @Pattern(regexp = "^[0-9]{2}$", message = "State code must be 2 digits (e.g. 27)")
    private String stateCode;

    @Pattern(regexp = "^$|^[1-9][0-9]{5}$", message = "PIN code must be 6 digits")
    private String pinCode;

    @Pattern(regexp = "^$|^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$", message = "Invalid GSTIN format")
    private String gstin;

    @NotBlank(message = "Customer type is required")
    @Pattern(regexp = "^(B2B|B2C)$", message = "Customer type must be B2B or B2C")
    private String customerType = "B2C";

    @DecimalMin(value = "0.0", inclusive = true, message = "Credit limit cannot be negative")
    private BigDecimal creditLimit = BigDecimal.ZERO;

    private String notes;
}