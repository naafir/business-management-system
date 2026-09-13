package com.antigravity.billing.dto.expense;

import com.antigravity.billing.entity.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class CreateExpenseRequest {

    private UUID categoryId;

    @NotNull(message = "Expense date is required")
    private LocalDate expenseDate;

    private String vendorName;

    @NotBlank(message = "Description is required")
    private String description;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    private BigDecimal amount;

    @Builder.Default
    private BigDecimal gstAmount = BigDecimal.ZERO;

    @Builder.Default
    private boolean gstEligible = false;

    private PaymentMethod paymentMethod;

    private String referenceNumber;

    private String notes;
}
