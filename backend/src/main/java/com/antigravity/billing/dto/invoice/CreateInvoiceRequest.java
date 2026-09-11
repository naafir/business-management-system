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
    private String customerName;
    private String customerGstin;
    private String placeOfSupplyState;
    private String placeOfSupplyCode;

    @NotNull(message = "Invoice date is required")
    private LocalDate invoiceDate;

    private LocalDate dueDate;

    @NotEmpty(message = "Line items cannot be empty")
    @Valid
    private List<InvoiceItemRequest> items;

    private BigDecimal amountPaid;
    private PaymentMethod paymentMethod;
    private String notes;
    private String termsAndConditions;
}
