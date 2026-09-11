package com.antigravity.billing.service;

import com.antigravity.billing.dto.common.PageResponse;
import com.antigravity.billing.dto.invoice.CreateInvoiceRequest;
import com.antigravity.billing.dto.invoice.InvoiceResponseDto;
import com.antigravity.billing.dto.invoice.InvoiceSummaryDto;
import com.antigravity.billing.entity.InvoiceStatus;
import com.antigravity.billing.entity.PaymentStatus;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.UUID;

public interface InvoiceService {
    PageResponse<InvoiceResponseDto> listInvoices(
            String search,
            UUID customerId,
            InvoiceStatus status,
            PaymentStatus paymentStatus,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable
    );

    InvoiceSummaryDto getInvoiceSummary();

    InvoiceResponseDto getInvoiceById(UUID id);

    InvoiceResponseDto getInvoiceByNumber(String invoiceNumber);

    InvoiceResponseDto createInvoice(CreateInvoiceRequest request, UUID userId, String username);

    InvoiceResponseDto createInvoiceFromSale(UUID saleId, UUID userId, String username);

    InvoiceResponseDto finalizeInvoice(UUID id);

    byte[] downloadInvoicePdf(UUID id);
}
