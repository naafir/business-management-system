package com.antigravity.billing.service;

import com.antigravity.billing.dto.payment.PaymentResponseDto;
import com.antigravity.billing.dto.payment.RecordPaymentRequest;

import java.util.List;
import java.util.UUID;

public interface PaymentService {

    PaymentResponseDto recordInvoicePayment(UUID invoiceId, RecordPaymentRequest request, UUID userId, String username);

    List<PaymentResponseDto> getInvoicePayments(UUID invoiceId);

    PaymentResponseDto recordSalePayment(UUID saleId, RecordPaymentRequest request, UUID userId, String username);

    List<PaymentResponseDto> getSalePayments(UUID saleId);
}
