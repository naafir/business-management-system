package com.antigravity.billing.dto.purchase;

import com.antigravity.billing.entity.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePurchaseRequest {

    @NotNull(message = "Supplier ID is required")
    private UUID supplierId;

    @jakarta.validation.constraints.Size(max = 100, message = "Supplier invoice number must not exceed 100 characters")
    private String supplierInvoiceNumber;

    @NotNull(message = "Purchase date is required")
    private LocalDate purchaseDate;

    @NotEmpty(message = "Purchase must contain at least one line item")
    @Valid
    private List<PurchaseItemRequest> items;

    @Builder.Default
    @DecimalMin(value = "0.00", message = "Amount paid cannot be negative")
    private BigDecimal amountPaid = BigDecimal.ZERO;

    private PaymentMethod paymentMethod;

    @jakarta.validation.constraints.Size(max = 1000, message = "Notes must not exceed 1000 characters")
    private String notes;
}
