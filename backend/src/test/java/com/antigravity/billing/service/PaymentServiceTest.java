package com.antigravity.billing.service;

import com.antigravity.billing.dto.payment.PaymentResponseDto;
import com.antigravity.billing.dto.payment.RecordPaymentRequest;
import com.antigravity.billing.entity.*;
import com.antigravity.billing.exception.ApiException;
import com.antigravity.billing.exception.ResourceNotFoundException;
import com.antigravity.billing.repository.CustomerRepository;
import com.antigravity.billing.repository.InvoiceRepository;
import com.antigravity.billing.repository.PaymentRepository;
import com.antigravity.billing.repository.SaleRepository;
import com.antigravity.billing.service.impl.PaymentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private SaleRepository saleRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private AuditService auditService;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private UUID invoiceId;
    private UUID saleId;
    private UUID customerId;
    private UUID userId;
    private Invoice testInvoice;
    private Sale testSale;
    private Customer testCustomer;

    @BeforeEach
    void setUp() {
        invoiceId = UUID.randomUUID();
        saleId = UUID.randomUUID();
        customerId = UUID.randomUUID();
        userId = UUID.randomUUID();

        testCustomer = Customer.builder()
                .name("Acme Corp")
                .outstandingBalance(new BigDecimal("1000.00"))
                .build();
        testCustomer.setId(customerId);

        testSale = Sale.builder()
                .saleNumber("SALE-001")
                .customer(testCustomer)
                .grandTotal(new BigDecimal("1000.00"))
                .amountPaid(BigDecimal.ZERO)
                .balanceDue(new BigDecimal("1000.00"))
                .paymentStatus(PaymentStatus.PENDING)
                .build();
        testSale.setId(saleId);

        testInvoice = Invoice.builder()
                .invoiceNumber("INV-001")
                .customerId(customerId)
                .saleId(saleId)
                .status(InvoiceStatus.FINALIZED)
                .grandTotal(new BigDecimal("1000.00"))
                .amountPaid(BigDecimal.ZERO)
                .balanceDue(new BigDecimal("1000.00"))
                .paymentStatus(PaymentStatus.PENDING)
                .build();
        testInvoice.setId(invoiceId);
    }

    @Test
    @DisplayName("A & B: Partial payment reduces balanceDue, increases amountPaid, sets status to PARTIALLY_PAID")
    void testPartialPaymentSuccess() {
        when(invoiceRepository.findByIdForUpdate(invoiceId)).thenReturn(Optional.of(testInvoice));
        when(saleRepository.findByIdForUpdate(saleId)).thenReturn(Optional.of(testSale));
        when(customerRepository.findByIdForUpdate(customerId)).thenReturn(Optional.of(testCustomer));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment p = invocation.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });

        RecordPaymentRequest request = RecordPaymentRequest.builder()
                .amountPaid(new BigDecimal("400.00"))
                .paymentMethod(PaymentMethod.CASH)
                .referenceNumber("REF123")
                .notes("Partial payment 1")
                .build();

        PaymentResponseDto dto = paymentService.recordInvoicePayment(invoiceId, request, userId, "admin");

        assertThat(dto).isNotNull();
        assertThat(dto.getAmount()).isEqualByComparingTo(new BigDecimal("400.00"));
        assertThat(testInvoice.getAmountPaid()).isEqualByComparingTo(new BigDecimal("400.00"));
        assertThat(testInvoice.getBalanceDue()).isEqualByComparingTo(new BigDecimal("600.00"));
        assertThat(testInvoice.getPaymentStatus()).isEqualTo(PaymentStatus.PARTIALLY_PAID);

        // H: Check linked sale synchronization
        assertThat(testSale.getAmountPaid()).isEqualByComparingTo(new BigDecimal("400.00"));
        assertThat(testSale.getBalanceDue()).isEqualByComparingTo(new BigDecimal("600.00"));
        assertThat(testSale.getPaymentStatus()).isEqualTo(PaymentStatus.PARTIALLY_PAID);

        // F: Check customer outstanding balance reduction
        assertThat(testCustomer.getOutstandingBalance()).isEqualByComparingTo(new BigDecimal("600.00"));
    }

    @Test
    @DisplayName("C: Full exact payment sets paymentStatus to PAID and balanceDue to 0.00")
    void testFullPaymentSuccess() {
        when(invoiceRepository.findByIdForUpdate(invoiceId)).thenReturn(Optional.of(testInvoice));
        when(saleRepository.findByIdForUpdate(saleId)).thenReturn(Optional.of(testSale));
        when(customerRepository.findByIdForUpdate(customerId)).thenReturn(Optional.of(testCustomer));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment p = invocation.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });

        RecordPaymentRequest request = RecordPaymentRequest.builder()
                .amountPaid(new BigDecimal("1000.00"))
                .paymentMethod(PaymentMethod.UPI)
                .build();

        PaymentResponseDto dto = paymentService.recordInvoicePayment(invoiceId, request, userId, "admin");

        assertThat(dto).isNotNull();
        assertThat(testInvoice.getAmountPaid()).isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(testInvoice.getBalanceDue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(testInvoice.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(testCustomer.getOutstandingBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("D: Overpayment attempt is rejected with ApiException")
    void testOverpaymentRejected() {
        when(invoiceRepository.findByIdForUpdate(invoiceId)).thenReturn(Optional.of(testInvoice));

        RecordPaymentRequest request = RecordPaymentRequest.builder()
                .amountPaid(new BigDecimal("1500.00"))
                .paymentMethod(PaymentMethod.CASH)
                .build();

        assertThatThrownBy(() -> paymentService.recordInvoicePayment(invoiceId, request, userId, "admin"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("exceeds remaining invoice balance");
    }

    @Test
    @DisplayName("E: Non-positive payment amount (<= 0) is rejected")
    void testNonPositivePaymentRejected() {
        when(invoiceRepository.findByIdForUpdate(invoiceId)).thenReturn(Optional.of(testInvoice));

        RecordPaymentRequest zeroReq = RecordPaymentRequest.builder()
                .amountPaid(BigDecimal.ZERO)
                .paymentMethod(PaymentMethod.CASH)
                .build();

        assertThatThrownBy(() -> paymentService.recordInvoicePayment(invoiceId, zeroReq, userId, "admin"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("must be greater than zero");

        RecordPaymentRequest negReq = RecordPaymentRequest.builder()
                .amountPaid(new BigDecimal("-100.00"))
                .paymentMethod(PaymentMethod.CASH)
                .build();

        assertThatThrownBy(() -> paymentService.recordInvoicePayment(invoiceId, negReq, userId, "admin"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("must be greater than zero");
    }

    @Test
    @DisplayName("I: Payment against non-existent invoice fails with ResourceNotFoundException")
    void testNonExistentInvoicePayment() {
        UUID randomId = UUID.randomUUID();
        when(invoiceRepository.findByIdForUpdate(randomId)).thenReturn(Optional.empty());

        RecordPaymentRequest request = RecordPaymentRequest.builder()
                .amountPaid(new BigDecimal("100.00"))
                .paymentMethod(PaymentMethod.CASH)
                .build();

        assertThatThrownBy(() -> paymentService.recordInvoicePayment(randomId, request, userId, "admin"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("J: Payment against CANCELLED invoice fails with ApiException")
    void testCancelledInvoicePaymentRejected() {
        testInvoice.setStatus(InvoiceStatus.CANCELLED);
        when(invoiceRepository.findByIdForUpdate(invoiceId)).thenReturn(Optional.of(testInvoice));

        RecordPaymentRequest request = RecordPaymentRequest.builder()
                .amountPaid(new BigDecimal("100.00"))
                .paymentMethod(PaymentMethod.CASH)
                .build();

        assertThatThrownBy(() -> paymentService.recordInvoicePayment(invoiceId, request, userId, "admin"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Cannot record payment for a cancelled invoice");
    }

    @Test
    @DisplayName("K: getInvoicePayments and getSalePayments return stored payment list")
    void testGetPaymentsList() {
        when(invoiceRepository.existsById(invoiceId)).thenReturn(true);
        Payment payment = Payment.builder()
                .paymentNumber("PAY-20260919-123456")
                .invoice(testInvoice)
                .amount(new BigDecimal("200.00"))
                .paymentMethod(PaymentMethod.BANK_TRANSFER)
                .paymentDate(ZonedDateTime.now())
                .build();
        payment.setId(UUID.randomUUID());

        when(paymentRepository.findByInvoiceIdOrderByPaymentDateDesc(invoiceId)).thenReturn(List.of(payment));

        List<PaymentResponseDto> list = paymentService.getInvoicePayments(invoiceId);
        assertThat(list).hasSize(1);
        assertThat(list.get(0).getPaymentNumber()).isEqualTo("PAY-20260919-123456");
        assertThat(list.get(0).getAmount()).isEqualByComparingTo(new BigDecimal("200.00"));
    }
}
