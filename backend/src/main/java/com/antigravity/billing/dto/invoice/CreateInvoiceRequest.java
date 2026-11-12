package com.antigravity.billing.dto.invoice;

import com.antigravity.billing.entity.PaymentMethod;
import jakarta.validation.Valid;
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
public class CreateInvoiceRequest {
    private UUID saleId;
    private UUID customerId;
    @jakarta.validation.constraints.Size(max = 200, message = "Customer name must not exceed 200 characters")
    private String customerName;

    @jakarta.validation.constraints.Size(max = 15, message = "Customer GSTIN must not exceed 15 characters")
    private String customerGstin;

    @jakarta.validation.constraints.Size(max = 100, message = "Place of supply state must not exceed 100 characters")
    private String placeOfSupplyState;

    @jakarta.validation.constraints.Size(max = 2, message = "Place of supply code must not exceed 2 characters")
    private String placeOfSupplyCode;

    @NotNull(message = "Invoice date is required")
    private LocalDate invoiceDate;

    private LocalDate dueDate;

    @NotEmpty(message = "Line items cannot be empty")
    @Valid
    private List<InvoiceItemRequest> items;

    @jakarta.validation.constraints.DecimalMin(value = "0.0", inclusive = true, message = "Amount paid cannot be negative")
    private BigDecimal amountPaid;
    private PaymentMethod paymentMethod;

    @jakarta.validation.constraints.Size(max = 1000, message = "Notes must not exceed 1000 characters")
    private String notes;

    @jakarta.validation.constraints.Size(max = 2000, message = "Terms and conditions must not exceed 2000 characters")
    private String termsAndConditions;
}
