package com.antigravity.billing.service;

import com.antigravity.billing.dto.inventory.InventorySummaryDto;
import com.antigravity.billing.dto.inventory.InventoryTransactionResponseDto;
import com.antigravity.billing.dto.inventory.StockAdjustmentRequest;
import com.antigravity.billing.dto.product.ProductResponseDto;
import com.antigravity.billing.entity.InventoryTransaction;
import com.antigravity.billing.entity.InventoryTransactionType;
import com.antigravity.billing.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public interface InventoryService {

    InventoryTransactionResponseDto adjustStock(StockAdjustmentRequest request, UUID userId, String username);

    InventoryTransaction recordTransaction(
            Product product,
            InventoryTransactionType type,
            BigDecimal quantity,
            BigDecimal previousStock,
            BigDecimal newStock,
            BigDecimal unitCost,
            String refType,
            UUID refId,
            String refNumber,
            String notes,
            UUID userId,
            String username
    );

    Page<InventoryTransactionResponseDto> getInventoryLedger(
            UUID productId,
            InventoryTransactionType type,
            Instant fromDate,
            Instant toDate,
            Pageable pageable
    );

    Page<InventoryTransactionResponseDto> getProductHistory(UUID productId, Pageable pageable);

    InventorySummaryDto getInventorySummary();

    Page<ProductResponseDto> getLowStockAlerts(Pageable pageable);

    Page<ProductResponseDto> getOutOfStockAlerts(Pageable pageable);
}
