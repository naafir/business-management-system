package com.antigravity.billing.dto.sale;

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
public class CreateSaleRequest {

    private UUID customerId;

    private String customerName;

    private String customerGstin;

    private String placeOfSupplyState;

    private String placeOfSupplyCode;

    @NotNull(message = "Sale date is required")
    private LocalDate saleDate;

    @NotEmpty(message = "Sale must contain at least one line item")
    @Valid
    private List<SaleItemRequest> items;

    @Builder.Default
    @DecimalMin(value = "0.00", message = "Amount paid cannot be negative")
    private BigDecimal amountPaid = BigDecimal.ZERO;

    private PaymentMethod paymentMethod;

    private String notes;
}
