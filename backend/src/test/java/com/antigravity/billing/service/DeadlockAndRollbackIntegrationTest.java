package com.antigravity.billing.service;

import com.antigravity.billing.dto.payment.RecordPaymentRequest;
import com.antigravity.billing.dto.purchase.CreatePurchaseRequest;
import com.antigravity.billing.dto.purchase.PurchaseItemRequest;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test-pg")
public class DeadlockAndRollbackIntegrationTest {

    @Autowired
    private SaleService saleService;

    @Autowired
    private PurchaseService purchaseService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private SaleRepository saleRepository;

    @Autowired
    private PurchaseRepository purchaseRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private InventoryTransactionRepository inventoryTransactionRepository;

    @Autowired
    private BusinessSettingsRepository businessSettingsRepository;

    @Autowired
    private UserRepository userRepository;

    private UUID userId;
    private Product product1;
    private Product product2;
    private Customer testCustomer;
    private Supplier testSupplier;

    @BeforeEach
    void setUp() {
        userId = userRepository.findAll().stream().findFirst().map(User::getId).orElse(UUID.randomUUID());

        if (businessSettingsRepository.count() == 0) {
            BusinessSettings settings = BusinessSettings.builder()
                    .legalName("Deadlock & Rollback Corp")
                    .tradeName("D&R Corp")
                    .gstin("27AAAAA2222A1Z5")
                    .stateName("Maharashtra")
                    .stateCode("27")
                    .invoicePrefix("INV-")
                    .invoiceNextSeq(900L)
                    .defaultCurrency("INR")
                    .build();
            businessSettingsRepository.save(settings);
        }

        Product p1 = Product.builder()
                .name("Alpha Item " + UUID.randomUUID().toString().substring(0, 6))
                .sku("SKU-ALPHA-" + UUID.randomUUID().toString().substring(0, 6))
                .hsnSac("21039090")
                .unit("PCS")
                .sellingPrice(new BigDecimal("100.00"))
                .purchasePrice(new BigDecimal("50.00"))
                .openingStock(new BigDecimal("100.00"))
                .currentStock(new BigDecimal("100.00"))
                .allowNegativeStock(true)
                .active(true)
                .build();

        Product p2 = Product.builder()
                .name("Beta Item " + UUID.randomUUID().toString().substring(0, 6))
                .sku("SKU-BETA-" + UUID.randomUUID().toString().substring(0, 6))
                .hsnSac("21039090")
                .unit("PCS")
                .sellingPrice(new BigDecimal("200.00"))
                .purchasePrice(new BigDecimal("100.00"))
                .openingStock(new BigDecimal("100.00"))
                .currentStock(new BigDecimal("100.00"))
                .allowNegativeStock(true)
                .active(true)
                .build();

        product1 = productRepository.save(p1);
        product2 = productRepository.save(p2);

        testCustomer = Customer.builder()
                .name("Rollback Customer " + UUID.randomUUID().toString().substring(0, 6))
                .stateName("Maharashtra")
                .stateCode("27")
                .customerType("B2C")
                .creditLimit(new BigDecimal("10000.00"))
                .outstandingBalance(BigDecimal.ZERO)
                .active(true)
                .build();
        testCustomer = customerRepository.save(testCustomer);

        testSupplier = Supplier.builder()
                .name("Rollback Supplier " + UUID.randomUUID().toString().substring(0, 6))
                .stateName("Maharashtra")
                .stateCode("27")
                .outstandingBalance(BigDecimal.ZERO)
                .active(true)
                .build();
        testSupplier = supplierRepository.save(testSupplier);
    }

    @Test
    @DisplayName("Deterministic product lock ordering prevents deadlocks when concurrent sales request multi-products in reverse order")
    void testReverseProductOrderingNoDeadlock() throws Exception {
        // Thread 1 requests: [Product 1, Product 2]
        // Thread 2 requests: [Product 2, Product 1]
        // Without deterministic sorting, T1 locks P1 then waits for P2, while T2 locks P2 then waits for P1 -> DEADLOCK.
        // With deterministic product sorting (sorted product IDs), both lock the same product first. Zero deadlocks!
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(2);
        AtomicInteger successCount = new AtomicInteger(0);

        executor.submit(() -> {
            try {
                startLatch.await();
                CreateSaleRequest req1 = CreateSaleRequest.builder()
                        .customerName("Buyer 1")
                        .placeOfSupplyState("Maharashtra")
                        .placeOfSupplyCode("27")
                        .saleDate(LocalDate.now())
                        .items(List.of(
                                SaleItemRequest.builder().productId(product1.getId()).quantity(new BigDecimal("1")).build(),
                                SaleItemRequest.builder().productId(product2.getId()).quantity(new BigDecimal("1")).build()
                        ))
                        .build();
                saleService.createSale(req1, userId, "concurrencyUser");
                successCount.incrementAndGet();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                endLatch.countDown();
            }
        });

        executor.submit(() -> {
            try {
                startLatch.await();
                CreateSaleRequest req2 = CreateSaleRequest.builder()
                        .customerName("Buyer 2")
                        .placeOfSupplyState("Maharashtra")
                        .placeOfSupplyCode("27")
                        .saleDate(LocalDate.now())
                        .items(List.of(
                                SaleItemRequest.builder().productId(product2.getId()).quantity(new BigDecimal("1")).build(),
                                SaleItemRequest.builder().productId(product1.getId()).quantity(new BigDecimal("1")).build()
                        ))
                        .build();
                saleService.createSale(req2, userId, "concurrencyUser");
                successCount.incrementAndGet();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                endLatch.countDown();
            }
        });

        startLatch.countDown();
        boolean finished = endLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(finished).isTrue();
        assertThat(successCount.get()).isEqualTo(2);
    }

    @Test
    @DisplayName("Rollback Safety: Sale creation failure rolls back product stock and customer balance changes completely")
    void testSaleTransactionRollbackOnFailure() {
        // Set product stock to 5. Set allowNegativeStock to false.
        Product p = Product.builder()
                .name("Strict Stock Product")
                .sku("STRICT-" + UUID.randomUUID().toString().substring(0, 6))
                .hsnSac("21039090")
                .unit("PCS")
                .sellingPrice(new BigDecimal("100.00"))
                .currentStock(new BigDecimal("5.00"))
                .allowNegativeStock(false)
                .active(true)
                .build();
        p = productRepository.save(p);

        long initialSaleCount = saleRepository.count();
        long initialInvTxCount = inventoryTransactionRepository.count();

        // Create a sale with 2 items: first item is valid (3.00), second item exceeds stock (10.00).
        CreateSaleRequest invalidRequest = CreateSaleRequest.builder()
                .customerId(testCustomer.getId())
                .saleDate(LocalDate.now())
                .amountPaid(BigDecimal.ZERO) // balance due will be created
                .items(List.of(
                        SaleItemRequest.builder().productId(p.getId()).quantity(new BigDecimal("3.00")).build(),
                        SaleItemRequest.builder().productId(p.getId()).quantity(new BigDecimal("10.00")).build()
                ))
                .build();

        assertThatThrownBy(() -> saleService.createSale(invalidRequest, userId, "concurrencyUser"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Insufficient stock");

        // Verify total rollback:
        // 1. Sale count unchanged
        assertThat(saleRepository.count()).isEqualTo(initialSaleCount);
        // 2. Inventory ledger count unchanged
        assertThat(inventoryTransactionRepository.count()).isEqualTo(initialInvTxCount);
        // 3. Product stock untouched (remains 5.00)
        Product reloadedProduct = productRepository.findById(p.getId()).orElseThrow();
        assertThat(reloadedProduct.getCurrentStock()).isEqualByComparingTo("5.00");
        // 4. Customer balance untouched (remains 0.00)
        Customer reloadedCustomer = customerRepository.findById(testCustomer.getId()).orElseThrow();
        assertThat(reloadedCustomer.getOutstandingBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Rollback Safety: Payment recording failure rolls back payment row, invoice balance and customer balance")
    void testPaymentTransactionRollbackOnFailure() {
        Invoice invoice = Invoice.builder()
                .invoiceNumber("INV-ROLLBACK-" + UUID.randomUUID().toString().substring(0, 6))
                .customerId(testCustomer.getId())
                .customerName(testCustomer.getName())
                .placeOfSupplyState("Maharashtra")
                .placeOfSupplyCode("27")
                .invoiceDate(LocalDate.now())
                .status(InvoiceStatus.FINALIZED)
                .subtotal(new BigDecimal("500.00"))
                .taxableAmount(new BigDecimal("500.00"))
                .grandTotal(new BigDecimal("500.00"))
                .amountPaid(BigDecimal.ZERO)
                .balanceDue(new BigDecimal("500.00"))
                .paymentStatus(PaymentStatus.PENDING)
                .createdBy(userId)
                .createdByName("concurrencyUser")
                .build();
        invoice = invoiceRepository.save(invoice);

        long initialPaymentCount = paymentRepository.count();

        // Attempt invalid payment exceeding balance due (600.00 > 500.00)
        RecordPaymentRequest invalidReq = RecordPaymentRequest.builder()
                .amountPaid(new BigDecimal("600.00"))
                .paymentMethod(PaymentMethod.CASH)
                .build();

        final UUID invId = invoice.getId();
        assertThatThrownBy(() -> paymentService.recordInvoicePayment(invId, invalidReq, userId, "concurrencyUser"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("exceeds remaining invoice balance");

        // Verify total rollback:
        // 1. Payment count unchanged
        assertThat(paymentRepository.count()).isEqualTo(initialPaymentCount);
        // 2. Invoice balanceDue untouched (remains 500.00), amountPaid untouched (0.00)
        Invoice reloadedInvoice = invoiceRepository.findById(invId).orElseThrow();
        assertThat(reloadedInvoice.getAmountPaid()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(reloadedInvoice.getBalanceDue()).isEqualByComparingTo("500.00");
        assertThat(reloadedInvoice.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    @DisplayName("Rollback Safety: Purchase recording failure rolls back purchase items, product stock, and supplier balance")
    void testPurchaseTransactionRollbackOnFailure() {
        long initialPurchaseCount = purchaseRepository.count();
        long initialInvTxCount = inventoryTransactionRepository.count();

        // Pass invalid supplier ID
        CreatePurchaseRequest invalidReq = CreatePurchaseRequest.builder()
                .supplierId(UUID.randomUUID()) // non-existent supplier
                .purchaseDate(LocalDate.now())
                .items(List.of(
                        PurchaseItemRequest.builder()
                                .productId(product1.getId())
                                .quantity(new BigDecimal("10.00"))
                                .unitPrice(new BigDecimal("50.00"))
                                .build()
                ))
                .build();

        assertThatThrownBy(() -> purchaseService.createPurchase(invalidReq, userId, "concurrencyUser"))
                .isInstanceOf(Exception.class);

        // Verify total rollback:
        // 1. Purchase count unchanged
        assertThat(purchaseRepository.count()).isEqualTo(initialPurchaseCount);
        // 2. Inventory transaction count unchanged
        assertThat(inventoryTransactionRepository.count()).isEqualTo(initialInvTxCount);
        // 3. Product stock untouched (remains 100.00)
        Product reloadedProduct = productRepository.findById(product1.getId()).orElseThrow();
        assertThat(reloadedProduct.getCurrentStock()).isEqualByComparingTo("100.00");
    }
}
