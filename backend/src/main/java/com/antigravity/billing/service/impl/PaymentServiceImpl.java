package com.antigravity.billing.service.impl;

import com.antigravity.billing.dto.payment.PaymentResponseDto;
import com.antigravity.billing.dto.payment.RecordPaymentRequest;
import com.antigravity.billing.entity.*;
import com.antigravity.billing.exception.ApiException;
import com.antigravity.billing.exception.ResourceNotFoundException;
import com.antigravity.billing.repository.CustomerRepository;
import com.antigravity.billing.repository.InvoiceRepository;
import com.antigravity.billing.repository.PaymentRepository;
import com.antigravity.billing.repository.SaleRepository;
import com.antigravity.billing.service.AuditService;
import com.antigravity.billing.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;
    private final SaleRepository saleRepository;
    private final CustomerRepository customerRepository;
    private final AuditService auditService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentResponseDto recordInvoicePayment(UUID invoiceId, RecordPaymentRequest request, UUID userId, String username) {
        Invoice invoice = invoiceRepository.findByIdForUpdate(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", "id", invoiceId));

        if (invoice.getStatus() == InvoiceStatus.CANCELLED) {
            throw new ApiException("Cannot record payment for a cancelled invoice", HttpStatus.BAD_REQUEST);
        }

        BigDecimal paymentAmount = request.getAmountPaid() != null ? request.getAmountPaid().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        if (paymentAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException("Payment amount must be greater than zero", HttpStatus.BAD_REQUEST);
        }

        BigDecimal currentBalance = invoice.getBalanceDue();
        if (paymentAmount.compareTo(currentBalance) > 0) {
            throw new ApiException("Payment amount ₹" + paymentAmount + " exceeds remaining invoice balance of ₹" + currentBalance, HttpStatus.BAD_REQUEST);
        }

        String paymentNumber = generatePaymentNumber();

        // Consistent lock order: Invoice -> Sale -> Customer
        Sale linkedSale = null;
        if (invoice.getSaleId() != null) {
            linkedSale = saleRepository.findByIdForUpdate(invoice.getSaleId()).orElse(null);
        }

        Customer customer = null;
        if (invoice.getCustomerId() != null) {
            customer = customerRepository.findByIdForUpdate(invoice.getCustomerId()).orElse(null);
        } else if (linkedSale != null && linkedSale.getCustomer() != null) {
            customer = customerRepository.findByIdForUpdate(linkedSale.getCustomer().getId()).orElse(null);
        }

        Payment payment = Payment.builder()
                .paymentNumber(paymentNumber)
                .invoice(invoice)
                .sale(linkedSale)
                .customer(customer)
                .amount(paymentAmount)
                .paymentMethod(request.getPaymentMethod())
                .paymentDate(ZonedDateTime.now())
                .referenceNumber(request.getReferenceNumber())
                .notes(request.getNotes())
                .createdBy(userId)
                .createdByName(username)
                .build();

        Payment savedPayment = paymentRepository.save(payment);

        // Calculate backend-authoritative balance & status from persistent payments
        BigDecimal newAmountPaid = invoice.getAmountPaid().add(paymentAmount).setScale(2, RoundingMode.HALF_UP);
        BigDecimal newBalanceDue = invoice.getGrandTotal().subtract(newAmountPaid).setScale(2, RoundingMode.HALF_UP);

        PaymentStatus newStatus;
        if (newBalanceDue.compareTo(BigDecimal.ZERO) <= 0) {
            newBalanceDue = BigDecimal.ZERO;
            newStatus = PaymentStatus.PAID;
        } else {
            newStatus = PaymentStatus.PARTIALLY_PAID;
        }

        invoice.setAmountPaid(newAmountPaid);
        invoice.setBalanceDue(newBalanceDue);
        invoice.setPaymentStatus(newStatus);
        if (request.getPaymentMethod() != null) {
            invoice.setPaymentMethod(request.getPaymentMethod());
        }
        invoiceRepository.save(invoice);

        // Synchronize linked Sale
        if (linkedSale != null) {
            linkedSale.setAmountPaid(newAmountPaid);
            linkedSale.setBalanceDue(newBalanceDue);
            linkedSale.setPaymentStatus(newStatus);
            if (request.getPaymentMethod() != null) {
                linkedSale.setPaymentMethod(request.getPaymentMethod());
            }
            saleRepository.save(linkedSale);
        }

        // Update customer balance consistently under pessimistic row lock
        if (customer != null) {
            BigDecimal currentCustBal = customer.getOutstandingBalance() != null ? customer.getOutstandingBalance() : BigDecimal.ZERO;
            BigDecimal updatedCustBal = currentCustBal.subtract(paymentAmount);
            if (updatedCustBal.compareTo(BigDecimal.ZERO) < 0) {
                updatedCustBal = BigDecimal.ZERO;
            }
            customer.setOutstandingBalance(updatedCustBal);
            customerRepository.save(customer);
        }

        auditService.logAction(
                userId, username,
                "RECORD_PAYMENT", "Invoice", invoice.getId().toString(),
                "Recorded payment " + paymentNumber + " of ₹" + paymentAmount + " for Invoice #" + invoice.getInvoiceNumber()
        );

        return mapToDto(savedPayment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponseDto> getInvoicePayments(UUID invoiceId) {
        if (!invoiceRepository.existsById(invoiceId)) {
            throw new ResourceNotFoundException("Invoice", "id", invoiceId);
        }
        return paymentRepository.findByInvoiceIdOrderByPaymentDateDesc(invoiceId).stream()
                .map(this::mapToDto)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentResponseDto recordSalePayment(UUID saleId, RecordPaymentRequest request, UUID userId, String username) {
        Invoice invoice = invoiceRepository.findBySaleId(saleId).orElse(null);
        if (invoice != null) {
            return recordInvoicePayment(invoice.getId(), request, userId, username);
        }

        // Lock Sale exclusively to prevent concurrent overpayment
        Sale sale = saleRepository.findByIdForUpdate(saleId)
                .orElseThrow(() -> new ResourceNotFoundException("Sale", "id", saleId));

        BigDecimal paymentAmount = request.getAmountPaid() != null ? request.getAmountPaid().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        if (paymentAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException("Payment amount must be greater than zero", HttpStatus.BAD_REQUEST);
        }

        BigDecimal currentBalance = sale.getBalanceDue();
        if (paymentAmount.compareTo(currentBalance) > 0) {
            throw new ApiException("Payment amount ₹" + paymentAmount + " exceeds remaining balance of ₹" + currentBalance, HttpStatus.BAD_REQUEST);
        }

        String paymentNumber = generatePaymentNumber();

        Customer customer = null;
        if (sale.getCustomer() != null) {
            customer = customerRepository.findByIdForUpdate(sale.getCustomer().getId()).orElse(null);
        }

        Payment payment = Payment.builder()
                .paymentNumber(paymentNumber)
                .sale(sale)
                .customer(customer)
                .amount(paymentAmount)
                .paymentMethod(request.getPaymentMethod())
                .paymentDate(ZonedDateTime.now())
                .referenceNumber(request.getReferenceNumber())
                .notes(request.getNotes())
                .createdBy(userId)
                .createdByName(username)
                .build();

        Payment savedPayment = paymentRepository.save(payment);

        BigDecimal newAmountPaid = sale.getAmountPaid().add(paymentAmount).setScale(2, RoundingMode.HALF_UP);
        BigDecimal newBalanceDue = sale.getGrandTotal().subtract(newAmountPaid).setScale(2, RoundingMode.HALF_UP);

        PaymentStatus newStatus;
        if (newBalanceDue.compareTo(BigDecimal.ZERO) <= 0) {
            newBalanceDue = BigDecimal.ZERO;
            newStatus = PaymentStatus.PAID;
        } else {
            newStatus = PaymentStatus.PARTIALLY_PAID;
        }

        sale.setAmountPaid(newAmountPaid);
        sale.setBalanceDue(newBalanceDue);
        sale.setPaymentStatus(newStatus);
        if (request.getPaymentMethod() != null) {
            sale.setPaymentMethod(request.getPaymentMethod());
        }
        saleRepository.save(sale);

        if (customer != null) {
            BigDecimal currentCustBal = customer.getOutstandingBalance() != null ? customer.getOutstandingBalance() : BigDecimal.ZERO;
            BigDecimal updatedCustBal = currentCustBal.subtract(paymentAmount);
            if (updatedCustBal.compareTo(BigDecimal.ZERO) < 0) {
                updatedCustBal = BigDecimal.ZERO;
            }
            customer.setOutstandingBalance(updatedCustBal);
            customerRepository.save(customer);
        }

        auditService.logAction(
                userId, username,
                "RECORD_PAYMENT", "Sale", sale.getId().toString(),
                "Recorded payment " + paymentNumber + " of ₹" + paymentAmount + " for Sale #" + sale.getSaleNumber()
        );

        return mapToDto(savedPayment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponseDto> getSalePayments(UUID saleId) {
        if (!saleRepository.existsById(saleId)) {
            throw new ResourceNotFoundException("Sale", "id", saleId);
        }
        return paymentRepository.findBySaleIdOrderByPaymentDateDesc(saleId).stream()
                .map(this::mapToDto)
                .toList();
    }

    private String generatePaymentNumber() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomSuffix = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return "PAY-" + dateStr + "-" + randomSuffix;
    }

    private PaymentResponseDto mapToDto(Payment payment) {
        return PaymentResponseDto.builder()
                .id(payment.getId())
                .paymentNumber(payment.getPaymentNumber())
                .invoiceId(payment.getInvoice() != null ? payment.getInvoice().getId() : null)
                .invoiceNumber(payment.getInvoice() != null ? payment.getInvoice().getInvoiceNumber() : null)
                .saleId(payment.getSale() != null ? payment.getSale().getId() : null)
                .saleNumber(payment.getSale() != null ? payment.getSale().getSaleNumber() : null)
                .customerId(payment.getCustomer() != null ? payment.getCustomer().getId() : null)
                .customerName(payment.getCustomer() != null ? payment.getCustomer().getName() : null)
                .amount(payment.getAmount())
                .paymentMethod(payment.getPaymentMethod())
                .paymentDate(payment.getPaymentDate())
                .referenceNumber(payment.getReferenceNumber())
                .notes(payment.getNotes())
                .createdBy(payment.getCreatedBy())
                .createdByName(payment.getCreatedByName())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}
