package com.antigravity.billing.service;

import com.antigravity.billing.dto.inventory.StockAdjustmentMode;
import com.antigravity.billing.dto.inventory.StockAdjustmentRequest;
import com.antigravity.billing.dto.sale.CreateSaleRequest;
import com.antigravity.billing.dto.sale.SaleItemRequest;
import com.antigravity.billing.entity.*;
import com.antigravity.billing.exception.ApiException;
import com.antigravity.billing.repository.BusinessSettingsRepository;
import com.antigravity.billing.repository.ProductRepository;
import com.antigravity.billing.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test-pg")
public class InventoryConcurrencyIntegrationTest {

    @Autowired
    private SaleService saleService;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private BusinessSettingsRepository businessSettingsRepository;

    @Autowired
    private UserRepository userRepository;

    private UUID userId;
    private Product testProduct;

    @BeforeEach
    void setUp() {
        userId = userRepository.findAll().stream().findFirst().map(User::getId).orElse(UUID.randomUUID());

        if (businessSettingsRepository.count() == 0) {
            BusinessSettings settings = BusinessSettings.builder()
                    .legalName("Inventory Concurrency Corp")
                    .tradeName("Inv Corp")
                    .gstin("27AAAAA0000A1Z5")
                    .stateName("Maharashtra")
                    .stateCode("27")
                    .invoicePrefix("INV-")
                    .invoiceNextSeq(500L)
                    .defaultCurrency("INR")
                    .build();
            businessSettingsRepository.save(settings);
        }

        Product product = Product.builder()
                .name("Oversell Protection Product " + UUID.randomUUID().toString().substring(0, 6))
                .sku("INV-CONC-" + UUID.randomUUID().toString().substring(0, 6))
                .hsnSac("21039090")
                .unit("PCS")
                .sellingPrice(new BigDecimal("100.00"))
                .purchasePrice(new BigDecimal("60.00"))
                .openingStock(new BigDecimal("10.00"))
                .currentStock(new BigDecimal("10.00"))
                .allowNegativeStock(false) // Strict non-negative stock enforced
                .active(true)
                .build();
        testProduct = productRepository.save(product);
    }

    @Test
    @DisplayName("C: Concurrent sales on the same product prevent overselling when stock is limited")
    void testConcurrentSalesPreventOverselling() throws Exception {
        // Stock = 10. Thread A wants 7. Thread B wants 6.
        // Total requested = 13 > 10. Exactly 1 must succeed, 1 must fail with insufficient stock.
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(2);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        BigDecimal[] requestedQuantities = { new BigDecimal("7.00"), new BigDecimal("6.00") };

        for (int i = 0; i < 2; i++) {
            final BigDecimal qty = requestedQuantities[i];
            executor.submit(() -> {
                try {
                    startLatch.await();
                    CreateSaleRequest request = CreateSaleRequest.builder()
                            .customerName("Walk-in Buyer")
                            .placeOfSupplyState("Maharashtra")
                            .placeOfSupplyCode("27")
                            .saleDate(LocalDate.now())
                            .paymentMethod(PaymentMethod.CASH)
                            .items(List.of(
                                    SaleItemRequest.builder()
                                            .productId(testProduct.getId())
                                            .quantity(qty)
                                            .unitPrice(new BigDecimal("100.00"))
                                            .discountPercent(BigDecimal.ZERO)
                                            .build()
                            ))
                            .build();

                    saleService.createSale(request, userId, "concurrencyUser");
                    successCount.incrementAndGet();
                } catch (ApiException e) {
                    if (e.getMessage().contains("Insufficient stock")) {
                        failureCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean finished = endLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(finished).isTrue();
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failureCount.get()).isEqualTo(1);

        Product reloadedProduct = productRepository.findById(testProduct.getId()).orElseThrow();
        // Stock was 10.00. Either 7.00 was sold (leaving 3.00) or 6.00 was sold (leaving 4.00).
        assertThat(reloadedProduct.getCurrentStock()).isGreaterThanOrEqualTo(BigDecimal.ZERO);
        assertThat(reloadedProduct.getCurrentStock()).isIn(new BigDecimal("3.00"), new BigDecimal("4.00"));
    }

    @Test
    @DisplayName("Concurrent stock adjustments execute safely under pessimistic write locking")
    void testConcurrentStockAdjustments() throws Exception {
        // Initial stock = 10. 5 threads increase stock by 2 each (total +10).
        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    StockAdjustmentRequest req = StockAdjustmentRequest.builder()
                            .productId(testProduct.getId())
                            .adjustmentMode(StockAdjustmentMode.INCREASE)
                            .quantity(new BigDecimal("2.00"))
                            .reason("Concurrent stock replenishment")
                            .build();

                    inventoryService.adjustStock(req, userId, "concurrencyUser");
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean finished = endLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(finished).isTrue();

        Product reloadedProduct = productRepository.findById(testProduct.getId()).orElseThrow();
        // 10 + (5 * 2) = 20.00
        assertThat(reloadedProduct.getCurrentStock()).isEqualByComparingTo("20.00");
    }
}
