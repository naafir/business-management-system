package com.antigravity.billing.service;

import com.antigravity.billing.dto.invoice.CreateInvoiceRequest;
import com.antigravity.billing.dto.invoice.InvoiceItemRequest;
import com.antigravity.billing.dto.invoice.InvoiceResponseDto;
import com.antigravity.billing.entity.BusinessSettings;
import com.antigravity.billing.entity.Product;
import com.antigravity.billing.entity.User;
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

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test-pg")
public class InvoiceConcurrencyIntegrationTest {

    @Autowired
    private InvoiceService invoiceService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private BusinessSettingsRepository businessSettingsRepository;

    @Autowired
    private UserRepository userRepository;

    private UUID productId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = userRepository.findAll().stream().findFirst().map(User::getId).orElse(null);

        if (businessSettingsRepository.count() == 0) {
            BusinessSettings settings = BusinessSettings.builder()
                    .legalName("Acme Global Corp")
                    .tradeName("Acme Retail")
                    .gstin("27AAACA1234A1Z5")
                    .stateName("Maharashtra")
                    .stateCode("27")
                    .invoicePrefix("INV-")
                    .invoiceNextSeq(100L)
                    .defaultCurrency("INR")
                    .build();
            businessSettingsRepository.save(settings);
        }

        Product product = Product.builder()
                .name("Concurrency Test Product")
                .sku("CONC-PROD-" + UUID.randomUUID().toString().substring(0, 6))
                .hsnSac("21039090")
                .unit("PCS")
                .sellingPrice(new BigDecimal("250.00"))
                .currentStock(new BigDecimal("1000"))
                .allowNegativeStock(true)
                .active(true)
                .build();
        Product savedProduct = productRepository.save(product);
        productId = savedProduct.getId();
    }

    @Test
    @DisplayName("Concurrent invoice creation produces 100% unique, non-colliding invoice numbers under load")
    void testConcurrentInvoiceNumberGeneration() throws Exception {
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(threadCount);

        List<Future<InvoiceResponseDto>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            futures.add(executor.submit(() -> {
                try {
                    latch.await();
                    CreateInvoiceRequest request = CreateInvoiceRequest.builder()
                            .customerName("Concurrent Customer " + index)
                            .placeOfSupplyState("Maharashtra")
                            .placeOfSupplyCode("27")
                            .invoiceDate(LocalDate.now())
                            .items(List.of(
                                    InvoiceItemRequest.builder()
                                            .productId(productId)
                                            .quantity(new BigDecimal("2"))
                                            .unitPrice(new BigDecimal("250.00"))
                                            .discountPercent(BigDecimal.ZERO)
                                            .build()
                            ))
                            .build();

                    return invoiceService.createInvoice(request, userId, "concurrencyUser");
                } finally {
                    completionLatch.countDown();
                }
            }));
        }

        // Release all threads simultaneously
        latch.countDown();
        boolean completed = completionLatch.await(15, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).isTrue();

        Set<String> generatedNumbers = ConcurrentHashMap.newKeySet();
        for (Future<InvoiceResponseDto> future : futures) {
            InvoiceResponseDto dto = future.get();
            assertThat(dto).isNotNull();
            assertThat(dto.getInvoiceNumber()).startsWith("INV-");
            generatedNumbers.add(dto.getInvoiceNumber());
        }

        // Assert 100% uniqueness among concurrent outputs
        assertThat(generatedNumbers).hasSize(threadCount);
    }
}
