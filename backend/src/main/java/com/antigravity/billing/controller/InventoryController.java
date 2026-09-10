package com.antigravity.billing.controller;

import com.antigravity.billing.dto.common.ApiResponse;
import com.antigravity.billing.dto.common.PageResponse;
import com.antigravity.billing.dto.inventory.InventorySummaryDto;
import com.antigravity.billing.dto.inventory.InventoryTransactionResponseDto;
import com.antigravity.billing.dto.inventory.StockAdjustmentRequest;
import com.antigravity.billing.dto.product.ProductResponseDto;
import com.antigravity.billing.entity.InventoryTransactionType;
import com.antigravity.billing.security.CustomUserDetails;
import com.antigravity.billing.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping("/ledger")
    public ResponseEntity<ApiResponse<PageResponse<InventoryTransactionResponseDto>>> getLedger(
            @RequestParam(required = false) UUID productId,
            @RequestParam(required = false) InventoryTransactionType type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<InventoryTransactionResponseDto> results = inventoryService.getInventoryLedger(
                productId, type, fromDate, toDate, pageable
        );
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.of(results)));
    }

    @GetMapping("/product/{productId}/history")
    public ResponseEntity<ApiResponse<PageResponse<InventoryTransactionResponseDto>>> getProductHistory(
            @PathVariable UUID productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<InventoryTransactionResponseDto> results = inventoryService.getProductHistory(productId, pageable);
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.of(results)));
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<InventorySummaryDto>> getSummary() {
        InventorySummaryDto summary = inventoryService.getInventorySummary();
        return ResponseEntity.ok(ApiResponse.ok(summary));
    }

    @GetMapping("/alerts/low-stock")
    public ResponseEntity<ApiResponse<PageResponse<ProductResponseDto>>> getLowStockAlerts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ProductResponseDto> results = inventoryService.getLowStockAlerts(pageable);
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.of(results)));
    }

    @GetMapping("/alerts/out-of-stock")
    public ResponseEntity<ApiResponse<PageResponse<ProductResponseDto>>> getOutOfStockAlerts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ProductResponseDto> results = inventoryService.getOutOfStockAlerts(pageable);
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.of(results)));
    }

    @PostMapping("/adjust")
    public ResponseEntity<ApiResponse<InventoryTransactionResponseDto>> adjustStock(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody StockAdjustmentRequest request
    ) {
        InventoryTransactionResponseDto result = inventoryService.adjustStock(
                request,
                userDetails.getId(),
                userDetails.getUsername()
        );
        return new ResponseEntity<>(ApiResponse.ok("Stock adjusted successfully", result), HttpStatus.CREATED);
    }
}
