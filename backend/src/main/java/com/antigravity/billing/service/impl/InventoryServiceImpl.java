package com.antigravity.billing.service.impl;

import com.antigravity.billing.dto.inventory.InventorySummaryDto;
import com.antigravity.billing.dto.inventory.InventoryTransactionResponseDto;
import com.antigravity.billing.dto.inventory.StockAdjustmentRequest;
import com.antigravity.billing.dto.product.ProductResponseDto;
import com.antigravity.billing.entity.InventoryTransaction;
import com.antigravity.billing.entity.InventoryTransactionType;
import com.antigravity.billing.entity.Product;
import com.antigravity.billing.exception.ApiException;
import com.antigravity.billing.exception.ResourceNotFoundException;
import com.antigravity.billing.repository.InventoryTransactionRepository;
import com.antigravity.billing.repository.ProductRepository;
import com.antigravity.billing.service.AuditService;
import com.antigravity.billing.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final ProductRepository productRepository;
    private final InventoryTransactionRepository transactionRepository;
    private final AuditService auditService;

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public InventoryTransactionResponseDto adjustStock(StockAdjustmentRequest request, UUID userId, String username) {
        // 1. Acquire exclusive lock on the product row
        Product product = productRepository.findByIdWithPessimisticLock(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", request.getProductId()));

        BigDecimal previousStock = product.getCurrentStock();
        BigDecimal requestedQty = request.getQuantity();
        BigDecimal newStock;
        BigDecimal quantityMagnitude;

        switch (request.getAdjustmentMode()) {
            case INCREASE -> {
                newStock = previousStock.add(requestedQty);
                quantityMagnitude = requestedQty;
            }
            case DECREASE -> {
                newStock = previousStock.subtract(requestedQty);
                quantityMagnitude = requestedQty;
            }
            case SET_EXACT -> {
                newStock = requestedQty;
                quantityMagnitude = newStock.subtract(previousStock).abs();
            }
            default -> throw new ApiException("Invalid adjustment mode", HttpStatus.BAD_REQUEST);
        }

        // 2. Validate negative stock constraint
        if (newStock.compareTo(BigDecimal.ZERO) < 0 && !product.isAllowNegativeStock()) {
            throw new ApiException(
                    "Insufficient stock for product '" + product.getName() + "'. Resulting stock would be "
                            + newStock + ", but negative stock is not allowed.",
                    HttpStatus.BAD_REQUEST
            );
        }

        // 3. Update product current stock
        product.setCurrentStock(newStock);
        productRepository.save(product);

        // 4. Calculate cost
        BigDecimal unitCost = request.getUnitCost() != null ? request.getUnitCost() : product.getPurchasePrice();
        BigDecimal totalCost = unitCost.multiply(quantityMagnitude);

        String fullNotes = request.getReason();
        if (request.getNotes() != null && !request.getNotes().isBlank()) {
            fullNotes += " - " + request.getNotes().trim();
        }

        // 5. Create immutable inventory transaction record
        InventoryTransaction transaction = InventoryTransaction.builder()
                .product(product)
                .transactionType(InventoryTransactionType.ADJUSTMENT)
                .quantity(quantityMagnitude)
                .previousStock(previousStock)
                .newStock(newStock)
                .unitCost(unitCost)
                .totalCost(totalCost)
                .referenceType("MANUAL_ADJUSTMENT")
                .referenceNumber("ADJ-" + System.currentTimeMillis())
                .notes(fullNotes)
                .createdBy(userId)
                .createdByName(username)
                .createdAt(Instant.now())
                .build();

        InventoryTransaction savedTransaction = transactionRepository.save(transaction);

        // 6. Audit log entry
        auditService.logAction(
                userId,
                username,
                "ADJUST_STOCK",
                "Product",
                product.getId().toString(),
                "Adjusted stock for '" + product.getName() + "' (SKU: " + product.getSku() + ") from "
                        + previousStock + " to " + newStock + " via " + request.getAdjustmentMode()
                        + " (Qty: " + quantityMagnitude + "). Reason: " + fullNotes
        );

        log.info("Stock adjusted for product {} (SKU: {}): {} -> {} by user {}",
                product.getId(), product.getSku(), previousStock, newStock, username);

        return mapToDto(savedTransaction);
    }

    @Override
    @Transactional
    public InventoryTransaction recordTransaction(
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
    ) {
        BigDecimal totalCost = (unitCost != null ? unitCost : BigDecimal.ZERO).multiply(quantity);

        InventoryTransaction transaction = InventoryTransaction.builder()
                .product(product)
                .transactionType(type)
                .quantity(quantity)
                .previousStock(previousStock)
                .newStock(newStock)
                .unitCost(unitCost != null ? unitCost : BigDecimal.ZERO)
                .totalCost(totalCost)
                .referenceType(refType)
                .referenceId(refId)
                .referenceNumber(refNumber)
                .notes(notes)
                .createdBy(userId)
                .createdByName(username)
                .createdAt(Instant.now())
                .build();

        return transactionRepository.save(transaction);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InventoryTransactionResponseDto> getInventoryLedger(
            UUID productId,
            InventoryTransactionType type,
            Instant fromDate,
            Instant toDate,
            Pageable pageable
    ) {
        return transactionRepository.findWithFilters(productId, type, fromDate, toDate, pageable)
                .map(this::mapToDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InventoryTransactionResponseDto> getProductHistory(UUID productId, Pageable pageable) {
        return transactionRepository.findByProductId(productId, pageable)
                .map(this::mapToDto);
    }

    @Override
    @Transactional(readOnly = true)
    public InventorySummaryDto getInventorySummary() {
        long totalProducts = productRepository.count();
        long lowStock = productRepository.countLowStockProducts();
        long outOfStock = productRepository.countOutOfStockProducts();
        BigDecimal totalValuation = productRepository.calculateTotalInventoryValuation();
        BigDecimal totalUnits = productRepository.calculateTotalUnitsInStock();

        return InventorySummaryDto.builder()
                .totalProductsCount(totalProducts)
                .lowStockCount(lowStock)
                .outOfStockCount(outOfStock)
                .totalValuation(totalValuation != null ? totalValuation : BigDecimal.ZERO)
                .totalUnitsInStock(totalUnits != null ? totalUnits : BigDecimal.ZERO)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponseDto> getLowStockAlerts(Pageable pageable) {
        return productRepository.findLowStockProducts(pageable)
                .map(this::mapToProductDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponseDto> getOutOfStockAlerts(Pageable pageable) {
        return productRepository.findOutOfStockProducts(pageable)
                .map(this::mapToProductDto);
    }

    private InventoryTransactionResponseDto mapToDto(InventoryTransaction tx) {
        Product p = tx.getProduct();
        return InventoryTransactionResponseDto.builder()
                .id(tx.getId())
                .productId(p != null ? p.getId() : null)
                .productSku(p != null ? p.getSku() : null)
                .productName(p != null ? p.getName() : "Unknown Product")
                .categoryName(p != null && p.getCategory() != null ? p.getCategory().getName() : "Uncategorized")
                .unit(p != null ? p.getUnit() : "PCS")
                .transactionType(tx.getTransactionType())
                .quantity(tx.getQuantity())
                .previousStock(tx.getPreviousStock())
                .newStock(tx.getNewStock())
                .unitCost(tx.getUnitCost())
                .totalCost(tx.getTotalCost())
                .referenceType(tx.getReferenceType())
                .referenceId(tx.getReferenceId())
                .referenceNumber(tx.getReferenceNumber())
                .notes(tx.getNotes())
                .createdBy(tx.getCreatedBy())
                .createdByName(tx.getCreatedByName())
                .createdAt(tx.getCreatedAt())
                .build();
    }

    private ProductResponseDto mapToProductDto(Product p) {
        boolean isLow = p.getCurrentStock().compareTo(p.getMinStockLevel()) <= 0;
        return ProductResponseDto.builder()
                .id(p.getId())
                .sku(p.getSku())
                .name(p.getName())
                .description(p.getDescription())
                .categoryId(p.getCategory() != null ? p.getCategory().getId() : null)
                .categoryName(p.getCategory() != null ? p.getCategory().getName() : "Uncategorized")
                .brand(p.getBrand())
                .hsnSac(p.getHsnSac())
                .unit(p.getUnit())
                .purchasePrice(p.getPurchasePrice())
                .sellingPrice(p.getSellingPrice())
                .gstRateId(p.getGstRate() != null ? p.getGstRate().getId() : null)
                .gstRatePercent(p.getGstRate() != null ? p.getGstRate().getRatePercent() : BigDecimal.ZERO)
                .openingStock(p.getOpeningStock())
                .currentStock(p.getCurrentStock())
                .minStockLevel(p.getMinStockLevel())
                .isLowStock(isLow)
                .allowNegativeStock(p.isAllowNegativeStock())
                .active(p.isActive())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}
