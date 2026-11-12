package com.antigravity.billing.service;

import com.antigravity.billing.dto.sale.CreateSaleRequest;
import com.antigravity.billing.dto.sale.SaleItemRequest;
import com.antigravity.billing.entity.*;
import com.antigravity.billing.exception.ApiException;
import com.antigravity.billing.repository.*;
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
public class CustomerBalanceConcurrencyIntegrationTest {

    @Autowired
    private SaleService saleService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private BusinessSettingsRepository businessSettingsRepository;

    @Autowired
    private UserRepository userRepository;

    private UUID userId;
    private Customer testCustomer;
    private Product testProduct;

    @BeforeEach
    void setUp() {
        userId = userRepository.findAll().stream().findFirst().map(User::getId).orElse(UUID.randomUUID());

        if (businessSettingsRepository.count() == 0) {
            BusinessSettings settings = BusinessSettings.builder()
                    .legalName("Customer Concurrency Corp")
                    .tradeName("Cust Corp")
                    .gstin("27AAAAA1111A1Z5")
                    .stateName("Maharashtra")
                    .stateCode("27")
                    .invoicePrefix("INV-")
                    .invoiceNextSeq(800L)
                    .defaultCurrency("INR")
                    .build();
            businessSettingsRepository.save(settings);
        }

        testCustomer = Customer.builder()
                .name("Credit Limit Customer " + UUID.randomUUID().toString().substring(0, 6))
                .stateName("Maharashtra")
                .stateCode("27")
                .customerType("B2B")
                .creditLimit(new BigDecimal("1000.00")) // Strict 1000.00 credit limit
                .outstandingBalance(BigDecimal.ZERO)
                .active(true)
                .build();
        testCustomer = customerRepository.save(testCustomer);

        testProduct = Product.builder()
                .name("Credit Check Product " + UUID.randomUUID().toString().substring(0, 6))
                .sku("CUST-PROD-" + UUID.randomUUID().toString().substring(0, 6))
                .hsnSac("21039090")
                .unit("PCS")
                .sellingPrice(new BigDecimal("800.00")) // Order balance due will be 800.00
                .currentStock(new BigDecimal("100.00"))
                .allowNegativeStock(true)
                .active(true)
                .build();
        testProduct = productRepository.save(testProduct);
    }

    @Test
    @DisplayName("D: Concurrent sales for customer enforce credit limit under race conditions")
    void testConcurrentSalesCreditLimitEnforcement() throws Exception {
        // Customer credit limit = 1000.00, starting balance = 0.00.
        // Two concurrent sales each for balance due 800.00.
        // Total requested debt = 1600.00 > 1000.00.
        // Under pessimistic customer locking, exactly 1 sale succeeds and 1 fails with Credit limit exceeded.
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(2);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < 2; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    CreateSaleRequest request = CreateSaleRequest.builder()
                            .customerId(testCustomer.getId())
                            .saleDate(LocalDate.now())
                            .amountPaid(BigDecimal.ZERO) // Entire 800.00 is balance due
                            .items(List.of(
                                    SaleItemRequest.builder()
                                            .productId(testProduct.getId())
                                            .quantity(new BigDecimal("1.00"))
                                            .unitPrice(new BigDecimal("800.00"))
                                            .discountPercent(BigDecimal.ZERO)
                                            .build()
                            ))
                            .build();

                    saleService.createSale(request, userId, "concurrencyUser");
                    successCount.incrementAndGet();
                } catch (ApiException e) {
                    if (e.getMessage().contains("Credit limit") && e.getMessage().contains("exceeded")) {
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

        Customer reloadedCustomer = customerRepository.findById(testCustomer.getId()).orElseThrow();
        // Customer outstanding balance must be exactly 800.00, strictly <= credit limit of 1000.00
        assertThat(reloadedCustomer.getOutstandingBalance()).isEqualByComparingTo("800.00");
    }
}
