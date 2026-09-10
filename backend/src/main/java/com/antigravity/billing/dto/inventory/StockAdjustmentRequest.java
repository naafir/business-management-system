package com.antigravity.billing.dto.inventory;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockAdjustmentRequest {

    @NotNull(message = "Product ID is required")
    private UUID productId;

    @NotNull(message = "Adjustment mode is required (INCREASE, DECREASE, SET_EXACT)")
    private StockAdjustmentMode adjustmentMode;

    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0.00", inclusive = false, message = "Quantity must be greater than zero")
    private BigDecimal quantity;

    @NotBlank(message = "Adjustment reason is required")
    private String reason;

    private String notes;

    private BigDecimal unitCost;
}
