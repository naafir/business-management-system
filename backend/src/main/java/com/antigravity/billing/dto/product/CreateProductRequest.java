package com.antigravity.billing.dto.product;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class CreateProductRequest {
    @NotBlank(message = "SKU is required")
    @Size(max = 50, message = "SKU must not exceed 50 characters")
    private String sku;

    @NotBlank(message = "Product name is required")
    @Size(max = 200, message = "Product name must not exceed 200 characters")
    private String name;

    private String description;
    private UUID categoryId;
    private String brand;

    @NotBlank(message = "HSN/SAC code is required")
    @Size(max = 20, message = "HSN/SAC code must not exceed 20 characters")
    private String hsnSac;

    @NotBlank(message = "Unit of measurement is required")
    private String unit = "PCS";

    @NotNull(message = "Purchase price is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Purchase price cannot be negative")
    private BigDecimal purchasePrice = BigDecimal.ZERO;

    @NotNull(message = "Selling price is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Selling price cannot be negative")
    private BigDecimal sellingPrice = BigDecimal.ZERO;

    private UUID gstRateId;

    @DecimalMin(value = "0.0", inclusive = true, message = "Opening stock cannot be negative")
    private BigDecimal openingStock = BigDecimal.ZERO;

    @DecimalMin(value = "0.0", inclusive = true, message = "Minimum stock level cannot be negative")
    private BigDecimal minStockLevel = new BigDecimal("5.00");

    private boolean allowNegativeStock = false;
}