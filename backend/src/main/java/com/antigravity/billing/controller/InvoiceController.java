package com.antigravity.billing.controller;

import com.antigravity.billing.dto.common.ApiResponse;
import com.antigravity.billing.dto.common.PageResponse;
import com.antigravity.billing.dto.invoice.CreateInvoiceRequest;
import com.antigravity.billing.dto.invoice.InvoiceResponseDto;
import com.antigravity.billing.dto.invoice.InvoiceSummaryDto;
import com.antigravity.billing.entity.InvoiceStatus;
import com.antigravity.billing.entity.PaymentStatus;
import com.antigravity.billing.security.CustomUserDetails;
import com.antigravity.billing.service.InvoiceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<InvoiceResponseDto>>> listInvoices(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) InvoiceStatus status,
            @RequestParam(required = false) PaymentStatus paymentStatus,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "invoiceDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        PageResponse<InvoiceResponseDto> response = invoiceService.listInvoices(search, customerId, status, paymentStatus, startDate, endDate, pageable);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<InvoiceSummaryDto>> getInvoiceSummary() {
        InvoiceSummaryDto summary = invoiceService.getInvoiceSummary();
        return ResponseEntity.ok(ApiResponse.ok(summary));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<InvoiceResponseDto>> getInvoiceById(@PathVariable UUID id) {
        InvoiceResponseDto invoice = invoiceService.getInvoiceById(id);
        return ResponseEntity.ok(ApiResponse.ok(invoice));
    }

    @GetMapping("/number/{invoiceNumber}")
    public ResponseEntity<ApiResponse<InvoiceResponseDto>> getInvoiceByNumber(@PathVariable String invoiceNumber) {
        InvoiceResponseDto invoice = invoiceService.getInvoiceByNumber(invoiceNumber);
        return ResponseEntity.ok(ApiResponse.ok(invoice));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<InvoiceResponseDto>> createInvoice(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CreateInvoiceRequest request
    ) {
        InvoiceResponseDto created = invoiceService.createInvoice(
                request,
                userDetails.getId(),
                userDetails.getUsername()
        );
        return new ResponseEntity<>(ApiResponse.ok("Invoice created successfully", created), HttpStatus.CREATED);
    }

    @PostMapping("/from-sale/{saleId}")
    public ResponseEntity<ApiResponse<InvoiceResponseDto>> createInvoiceFromSale(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID saleId
    ) {
        InvoiceResponseDto created = invoiceService.createInvoiceFromSale(
                saleId,
                userDetails.getId(),
                userDetails.getUsername()
        );
        return new ResponseEntity<>(ApiResponse.ok("Invoice generated from sale", created), HttpStatus.CREATED);
    }

    @PutMapping("/{id}/finalize")
    public ResponseEntity<ApiResponse<InvoiceResponseDto>> finalizeInvoice(@PathVariable UUID id) {
        InvoiceResponseDto finalized = invoiceService.finalizeInvoice(id);
        return ResponseEntity.ok(ApiResponse.ok("Invoice finalized successfully", finalized));
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> downloadInvoicePdf(@PathVariable UUID id) {
        byte[] pdfBytes = invoiceService.downloadInvoicePdf(id);
        InvoiceResponseDto invoice = invoiceService.getInvoiceById(id);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("inline", "Invoice-" + invoice.getInvoiceNumber() + ".pdf");
        headers.setContentLength(pdfBytes.length);

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }
}
