package com.antigravity.billing.dto.inventory;

import com.antigravity.billing.entity.InventoryTransactionType;
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
public class InventoryTransactionResponseDto {
    private UUID id;
    private UUID productId;
    private String productSku;
    private String productName;
    private String categoryName;
    private String unit;
    private InventoryTransactionType transactionType;
    private BigDecimal quantity;
    private BigDecimal previousStock;
    private BigDecimal newStock;
    private BigDecimal unitCost;
    private BigDecimal totalCost;
    private String referenceType;
    private UUID referenceId;
    private String referenceNumber;
    private String notes;
    private UUID createdBy;
    private String createdByName;
    private Instant createdAt;
}
