package com.antigravity.billing.dto.expense;

import com.antigravity.billing.entity.PaymentMethod;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class ExpenseResponseDto {
    private UUID id;
    private UUID categoryId;
    private String categoryName;
    private LocalDate expenseDate;
    private String vendorName;
    private String description;
    private BigDecimal amount;
    private BigDecimal gstAmount;
    private BigDecimal totalAmount;
    private boolean gstEligible;
    private PaymentMethod paymentMethod;
    private String referenceNumber;
    private String notes;
    private String createdByName;
    private Instant createdAt;
    private Instant updatedAt;
}
