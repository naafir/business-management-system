package com.antigravity.billing.dto.product;

import com.fasterxml.jackson.annotation.JsonProperty;
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
public class ProductResponseDto {
    private UUID id;
    private String sku;
    private String name;
    private String description;
    private UUID categoryId;
    private String categoryName;
    private String brand;
    private String hsnSac;
    private String unit;
    private BigDecimal purchasePrice;
    private BigDecimal sellingPrice;
    private UUID gstRateId;
    private BigDecimal gstRatePercent;
    private BigDecimal openingStock;
    private BigDecimal currentStock;
    private BigDecimal minStockLevel;
    @JsonProperty("isLowStock")
    private boolean isLowStock;
    @JsonProperty("lowStock")
    public boolean isLowStockAlias() {
        return isLowStock;
    }
    private boolean allowNegativeStock;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;
}