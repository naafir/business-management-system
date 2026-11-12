package com.antigravity.billing.service;

import com.antigravity.billing.dto.payment.PaymentResponseDto;
import com.antigravity.billing.dto.payment.RecordPaymentRequest;
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
public class PaymentConcurrencyIntegrationTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private SaleRepository saleRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private UserRepository userRepository;

    private UUID userId;
    private Customer testCustomer;
    private Invoice testInvoice;

    @BeforeEach
    void setUp() {
        userId = userRepository.findAll().stream().findFirst().map(User::getId).orElse(UUID.randomUUID());

        testCustomer = Customer.builder()
                .name("Payment Concurrency Customer " + UUID.randomUUID().toString().substring(0, 6))
                .stateName("Maharashtra")
                .stateCode("27")
                .customerType("B2C")
                .creditLimit(new BigDecimal("10000.00"))
                .outstandingBalance(new BigDecimal("1000.00"))
                .active(true)
                .build();
        testCustomer = customerRepository.save(testCustomer);

        Sale sale = Sale.builder()
                .saleNumber("SALE-CONC-" + UUID.randomUUID().toString().substring(0, 6))
                .customer(testCustomer)
                .customerName(testCustomer.getName())
                .placeOfSupplyState("Maharashtra")
                .placeOfSupplyCode("27")
                .saleDate(LocalDate.now())
                .subtotal(new BigDecimal("1000.00"))
                .taxableAmount(new BigDecimal("1000.00"))
                .totalTax(BigDecimal.ZERO)
                .grandTotal(new BigDecimal("1000.00"))
                .amountPaid(BigDecimal.ZERO)
                .balanceDue(new BigDecimal("1000.00"))
                .paymentStatus(PaymentStatus.PENDING)
                .createdBy(userId)
                .createdByName("concurrencyUser")
                .build();
        sale = saleRepository.save(sale);

        testInvoice = Invoice.builder()
                .invoiceNumber("INV-CONC-" + UUID.randomUUID().toString().substring(0, 6))
                .saleId(sale.getId())
                .customerId(testCustomer.getId())
                .customerName(testCustomer.getName())
                .placeOfSupplyState("Maharashtra")
                .placeOfSupplyCode("27")
                .invoiceDate(LocalDate.now())
                .status(InvoiceStatus.FINALIZED)
                .subtotal(new BigDecimal("1000.00"))
                .taxableAmount(new BigDecimal("1000.00"))
                .grandTotal(new BigDecimal("1000.00"))
                .amountPaid(BigDecimal.ZERO)
                .balanceDue(new BigDecimal("1000.00"))
                .paymentStatus(PaymentStatus.PENDING)
                .createdBy(userId)
                .createdByName("concurrencyUser")
                .build();
        testInvoice = invoiceRepository.save(testInvoice);
    }

    @Test
    @DisplayName("A & B: Two concurrent payment attempts exceeding remaining balance reject overpayment safely")
    void testConcurrentPaymentsOverpaymentProtection() throws Exception {
        // Invoice balance is 1000. Two threads both try to pay 600 simultaneously.
        // Total requested = 1200 > 1000. Exactly 1 must succeed, 1 must be rejected.
        int threadCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    RecordPaymentRequest req = RecordPaymentRequest.builder()
                            .amountPaid(new BigDecimal("600.00"))
                            .paymentMethod(PaymentMethod.CASH)
                            .notes("Concurrent payment attempt")
                            .build();

                    paymentService.recordInvoicePayment(testInvoice.getId(), req, userId, "concurrencyUser");
                    successCount.incrementAndGet();
                } catch (ApiException e) {
                    if (e.getMessage().contains("exceeds remaining invoice balance")) {
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

        Invoice reloadedInvoice = invoiceRepository.findById(testInvoice.getId()).orElseThrow();
        assertThat(reloadedInvoice.getAmountPaid()).isEqualByComparingTo("600.00");
        assertThat(reloadedInvoice.getBalanceDue()).isEqualByComparingTo("400.00");
        assertThat(reloadedInvoice.getPaymentStatus()).isEqualTo(PaymentStatus.PARTIALLY_PAID);

        List<Payment> persistedPayments = paymentRepository.findByInvoiceIdOrderByPaymentDateDesc(testInvoice.getId());
        assertThat(persistedPayments).hasSize(1);
        assertThat(persistedPayments.get(0).getAmount()).isEqualByComparingTo("600.00");

        Customer reloadedCustomer = customerRepository.findById(testCustomer.getId()).orElseThrow();
        // Initial customer balance was 1000.00, reduced by 600.00 -> 400.00
        assertThat(reloadedCustomer.getOutstandingBalance()).isEqualByComparingTo("400.00");
    }

    @Test
    @DisplayName("Two concurrent payments summing exactly to grand total both succeed without balance corruption")
    void testConcurrentExactSplitPayments() throws Exception {
        // Invoice balance is 1000. Thread 1 pays 600, Thread 2 pays 400 simultaneously.
        // Total requested = 1000. Both should succeed, final balanceDue = 0, status = PAID.
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(2);

        BigDecimal[] amounts = { new BigDecimal("600.00"), new BigDecimal("400.00") };

        for (int i = 0; i < 2; i++) {
            final BigDecimal amt = amounts[i];
            executor.submit(() -> {
                try {
                    startLatch.await();
                    RecordPaymentRequest req = RecordPaymentRequest.builder()
                            .amountPaid(amt)
                            .paymentMethod(PaymentMethod.BANK_TRANSFER)
                            .notes("Split payment")
                            .build();

                    paymentService.recordInvoicePayment(testInvoice.getId(), req, userId, "concurrencyUser");
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

        Invoice reloadedInvoice = invoiceRepository.findById(testInvoice.getId()).orElseThrow();
        assertThat(reloadedInvoice.getAmountPaid()).isEqualByComparingTo("1000.00");
        assertThat(reloadedInvoice.getBalanceDue()).isEqualByComparingTo("0.00");
        assertThat(reloadedInvoice.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);

        List<Payment> persistedPayments = paymentRepository.findByInvoiceIdOrderByPaymentDateDesc(testInvoice.getId());
        assertThat(persistedPayments).hasSize(2);

        Customer reloadedCustomer = customerRepository.findById(testCustomer.getId()).orElseThrow();
        assertThat(reloadedCustomer.getOutstandingBalance()).isEqualByComparingTo("0.00");
    }
}
