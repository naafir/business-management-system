package com.antigravity.billing.dto.inventory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventorySummaryDto {
    private long totalProductsCount;
    private long lowStockCount;
    private long outOfStockCount;
    private BigDecimal totalValuation;
    private BigDecimal totalUnitsInStock;
}
