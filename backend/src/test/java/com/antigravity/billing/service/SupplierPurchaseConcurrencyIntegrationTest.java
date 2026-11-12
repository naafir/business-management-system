package com.antigravity.billing.service;

import com.antigravity.billing.dto.purchase.CreatePurchaseRequest;
import com.antigravity.billing.dto.purchase.PurchaseItemRequest;
import com.antigravity.billing.entity.*;
import com.antigravity.billing.repository.BusinessSettingsRepository;
import com.antigravity.billing.repository.ProductRepository;
import com.antigravity.billing.repository.SupplierRepository;
import com.antigravity.billing.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test-pg")
public class SupplierPurchaseConcurrencyIntegrationTest {

    @Autowired
    private PurchaseService purchaseService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private BusinessSettingsRepository businessSettingsRepository;

    @Autowired
    private UserRepository userRepository;

    private UUID userId;
    private Product testProduct;
    private Supplier testSupplier;

    @BeforeEach
    void setUp() {
        userId = userRepository.findAll().stream().findFirst().map(User::getId).orElse(UUID.randomUUID());

        if (businessSettingsRepository.count() == 0) {
            BusinessSettings settings = BusinessSettings.builder()
                    .legalName("Purchase Concurrency Corp")
                    .tradeName("Purchase Corp")
                    .gstin("27AAAAA0000A1Z5")
                    .stateName("Maharashtra")
                    .stateCode("27")
                    .invoicePrefix("INV-")
                    .invoiceNextSeq(500L)
                    .defaultCurrency("INR")
                    .build();
            businessSettingsRepository.save(settings);
        }

        Supplier supplier = Supplier.builder()
                .name("Global Tech Supplies")
                .gstin("27SUPPLIER0001Z")
                .stateName("Maharashtra")
                .stateCode("27")
                .outstandingBalance(BigDecimal.ZERO)
                .active(true)
                .build();
        testSupplier = supplierRepository.save(supplier);

        Product product = Product.builder()
                .name("High-End Server Component")
                .sku("SRV-COMP-" + UUID.randomUUID().toString().substring(0, 6))
                .hsnSac("8471")
                .unit("PCS")
                .sellingPrice(new BigDecimal("5000.00"))
                .purchasePrice(new BigDecimal("4000.00"))
                .openingStock(BigDecimal.ZERO)
                .currentStock(BigDecimal.ZERO)
                .allowNegativeStock(false)
                .active(true)
                .build();
        testProduct = productRepository.save(product);
    }

    @Test
    @DisplayName("Concurrent purchases from the same supplier correctly update balances and stock without deadlocks")
    void testConcurrentPurchasesForSameSupplier() throws Exception {
        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final String invoiceNo = "BILL-" + UUID.randomUUID().toString().substring(0, 6);
            executor.submit(() -> {
                try {
                    startLatch.await();
                    CreatePurchaseRequest request = CreatePurchaseRequest.builder()
                            .supplierId(testSupplier.getId())
                            .supplierInvoiceNumber(invoiceNo)
                            .purchaseDate(LocalDate.now())
                            .items(List.of(
                                    PurchaseItemRequest.builder()
                                            .productId(testProduct.getId())
                                            .quantity(new BigDecimal("10.00"))
                                            .unitPrice(new BigDecimal("4000.00"))
                                            .discountPercent(BigDecimal.ZERO)
                                            .build()
                            ))
                            .build();

                    purchaseService.createPurchase(request, userId, "concurrencyUser");
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean finished = endLatch.await(15, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(finished).isTrue();
        assertThat(successCount.get()).isEqualTo(5);

        // Verify Product Stock (10 * 5 = 50)
        Product reloadedProduct = productRepository.findById(testProduct.getId()).orElseThrow();
        assertThat(reloadedProduct.getCurrentStock()).isEqualByComparingTo("50.00");

        // Verify Supplier Balance (4000 * 10 * 5 = 200,000. GST is 0%)
        Supplier reloadedSupplier = supplierRepository.findById(testSupplier.getId()).orElseThrow();
        assertThat(reloadedSupplier.getOutstandingBalance()).isEqualByComparingTo("200000.00");
    }
}
