package com.antigravity.billing.service;

import com.antigravity.billing.dto.inventory.InventorySummaryDto;
import com.antigravity.billing.dto.inventory.InventoryTransactionResponseDto;
import com.antigravity.billing.dto.inventory.StockAdjustmentMode;
import com.antigravity.billing.dto.inventory.StockAdjustmentRequest;
import com.antigravity.billing.entity.InventoryTransaction;
import com.antigravity.billing.entity.InventoryTransactionType;
import com.antigravity.billing.entity.Product;
import com.antigravity.billing.exception.ApiException;
import com.antigravity.billing.repository.InventoryTransactionRepository;
import com.antigravity.billing.repository.ProductRepository;
import com.antigravity.billing.service.impl.InventoryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private InventoryTransactionRepository transactionRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private InventoryServiceImpl inventoryService;

    private Product testProduct;
    private UUID productId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();
        userId = UUID.randomUUID();

        testProduct = Product.builder()
                .sku("FOOD-SAUCE-01")
                .name("Dark Soya Sauce 500ml")
                .unit("BTL")
                .purchasePrice(new BigDecimal("80.00"))
                .sellingPrice(new BigDecimal("120.00"))
                .openingStock(new BigDecimal("50.00"))
                .currentStock(new BigDecimal("50.00"))
                .minStockLevel(new BigDecimal("10.00"))
                .allowNegativeStock(false)
                .active(true)
                .build();
        testProduct.setId(productId);
    }

    @Test
    @DisplayName("Successfully increases stock and logs ADJUSTMENT transaction")
    void testAdjustStockIncrease() {
        StockAdjustmentRequest request = StockAdjustmentRequest.builder()
                .productId(productId)
                .adjustmentMode(StockAdjustmentMode.INCREASE)
                .quantity(new BigDecimal("20.00"))
                .reason("New batch delivery recount")
                .build();

        when(productRepository.findByIdWithPessimisticLock(productId)).thenReturn(Optional.of(testProduct));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.save(any(InventoryTransaction.class))).thenAnswer(inv -> {
            InventoryTransaction tx = inv.getArgument(0);
            tx.setId(UUID.randomUUID());
            return tx;
        });

        InventoryTransactionResponseDto response = inventoryService.adjustStock(request, userId, "admin");

        assertThat(response).isNotNull();
        assertThat(response.getPreviousStock()).isEqualByComparingTo("50.00");
        assertThat(response.getNewStock()).isEqualByComparingTo("70.00");
        assertThat(response.getQuantity()).isEqualByComparingTo("20.00");
        assertThat(response.getTransactionType()).isEqualTo(InventoryTransactionType.ADJUSTMENT);
        assertThat(testProduct.getCurrentStock()).isEqualByComparingTo("70.00");

        verify(auditService, times(1)).logAction(eq(userId), eq("admin"), eq("ADJUST_STOCK"), eq("Product"), anyString(), anyString());
    }

    @Test
    @DisplayName("Successfully decreases stock and logs ADJUSTMENT transaction")
    void testAdjustStockDecrease() {
        StockAdjustmentRequest request = StockAdjustmentRequest.builder()
                .productId(productId)
                .adjustmentMode(StockAdjustmentMode.DECREASE)
                .quantity(new BigDecimal("15.00"))
                .reason("Damaged bottles written off")
                .notes("Broken during shelf restocking")
                .build();

        when(productRepository.findByIdWithPessimisticLock(productId)).thenReturn(Optional.of(testProduct));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.save(any(InventoryTransaction.class))).thenAnswer(inv -> {
            InventoryTransaction tx = inv.getArgument(0);
            tx.setId(UUID.randomUUID());
            return tx;
        });

        InventoryTransactionResponseDto response = inventoryService.adjustStock(request, userId, "admin");

        assertThat(response).isNotNull();
        assertThat(response.getPreviousStock()).isEqualByComparingTo("50.00");
        assertThat(response.getNewStock()).isEqualByComparingTo("35.00");
        assertThat(response.getQuantity()).isEqualByComparingTo("15.00");
        assertThat(testProduct.getCurrentStock()).isEqualByComparingTo("35.00");
    }

    @Test
    @DisplayName("Rejects stock decrease that results in negative stock when allowNegativeStock is false")
    void testAdjustStockRejectsNegativeStock() {
        StockAdjustmentRequest request = StockAdjustmentRequest.builder()
                .productId(productId)
                .adjustmentMode(StockAdjustmentMode.DECREASE)
                .quantity(new BigDecimal("60.00")) // current is 50 -> would be -10
                .reason("Large manual deduction")
                .build();

        when(productRepository.findByIdWithPessimisticLock(productId)).thenReturn(Optional.of(testProduct));

        assertThatThrownBy(() -> inventoryService.adjustStock(request, userId, "admin"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Insufficient stock");

        verify(productRepository, never()).save(any(Product.class));
        verify(transactionRepository, never()).save(any(InventoryTransaction.class));
    }

    @Test
    @DisplayName("Allows stock decrease into negative when allowNegativeStock is true")
    void testAdjustStockAllowsNegativeStockWhenConfigured() {
        testProduct.setAllowNegativeStock(true);

        StockAdjustmentRequest request = StockAdjustmentRequest.builder()
                .productId(productId)
                .adjustmentMode(StockAdjustmentMode.DECREASE)
                .quantity(new BigDecimal("60.00"))
                .reason("Emergency customer dispatch prior to goods receipt")
                .build();

        when(productRepository.findByIdWithPessimisticLock(productId)).thenReturn(Optional.of(testProduct));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.save(any(InventoryTransaction.class))).thenAnswer(inv -> {
            InventoryTransaction tx = inv.getArgument(0);
            tx.setId(UUID.randomUUID());
            return tx;
        });

        InventoryTransactionResponseDto response = inventoryService.adjustStock(request, userId, "admin");

        assertThat(response.getNewStock()).isEqualByComparingTo("-10.00");
        assertThat(testProduct.getCurrentStock()).isEqualByComparingTo("-10.00");
    }

    @Test
    @DisplayName("Sets exact stock quantity and calculates correct delta")
    void testAdjustStockSetExact() {
        StockAdjustmentRequest request = StockAdjustmentRequest.builder()
                .productId(productId)
                .adjustmentMode(StockAdjustmentMode.SET_EXACT)
                .quantity(new BigDecimal("42.00")) // current is 50 -> delta magnitude is 8
                .reason("Year-end physical inventory audit count")
                .build();

        when(productRepository.findByIdWithPessimisticLock(productId)).thenReturn(Optional.of(testProduct));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.save(any(InventoryTransaction.class))).thenAnswer(inv -> {
            InventoryTransaction tx = inv.getArgument(0);
            tx.setId(UUID.randomUUID());
            return tx;
        });

        InventoryTransactionResponseDto response = inventoryService.adjustStock(request, userId, "admin");

        assertThat(response.getPreviousStock()).isEqualByComparingTo("50.00");
        assertThat(response.getNewStock()).isEqualByComparingTo("42.00");
        assertThat(response.getQuantity()).isEqualByComparingTo("8.00");
        assertThat(testProduct.getCurrentStock()).isEqualByComparingTo("42.00");
    }

    @Test
    @DisplayName("Calculates inventory summary metrics accurately")
    void testGetInventorySummary() {
        when(productRepository.count()).thenReturn(15L);
        when(productRepository.countLowStockProducts()).thenReturn(3L);
        when(productRepository.countOutOfStockProducts()).thenReturn(1L);
        when(productRepository.calculateTotalInventoryValuation()).thenReturn(new BigDecimal("125000.00"));
        when(productRepository.calculateTotalUnitsInStock()).thenReturn(new BigDecimal("850.00"));

        InventorySummaryDto summary = inventoryService.getInventorySummary();

        assertThat(summary.getTotalProductsCount()).isEqualTo(15L);
        assertThat(summary.getLowStockCount()).isEqualTo(3L);
        assertThat(summary.getOutOfStockCount()).isEqualTo(1L);
        assertThat(summary.getTotalValuation()).isEqualByComparingTo("125000.00");
        assertThat(summary.getTotalUnitsInStock()).isEqualByComparingTo("850.00");
    }
}
