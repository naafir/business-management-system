package com.antigravity.billing.controller;

import com.antigravity.billing.dto.common.ApiResponse;
import com.antigravity.billing.dto.common.PageResponse;
import com.antigravity.billing.dto.purchase.CreatePurchaseRequest;
import com.antigravity.billing.dto.purchase.PurchaseResponseDto;
import com.antigravity.billing.dto.purchase.PurchaseSummaryDto;
import com.antigravity.billing.entity.PaymentStatus;
import com.antigravity.billing.security.CustomUserDetails;
import com.antigravity.billing.service.PurchaseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/purchases")
@RequiredArgsConstructor
public class PurchaseController {

    private final PurchaseService purchaseService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<PurchaseResponseDto>>> listPurchases(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UUID supplierId,
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "purchaseDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        PageResponse<PurchaseResponseDto> response = purchaseService.listPurchases(search, supplierId, status, startDate, endDate, pageable);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<PurchaseSummaryDto>> getPurchaseSummary() {
        PurchaseSummaryDto summary = purchaseService.getPurchaseSummary();
        return ResponseEntity.ok(ApiResponse.ok(summary));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PurchaseResponseDto>> getPurchaseById(@PathVariable UUID id) {
        PurchaseResponseDto purchase = purchaseService.getPurchaseById(id);
        return ResponseEntity.ok(ApiResponse.ok(purchase));
    }

    @GetMapping("/number/{purchaseNumber}")
    public ResponseEntity<ApiResponse<PurchaseResponseDto>> getPurchaseByNumber(@PathVariable String purchaseNumber) {
        PurchaseResponseDto purchase = purchaseService.getPurchaseByNumber(purchaseNumber);
        return ResponseEntity.ok(ApiResponse.ok(purchase));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PurchaseResponseDto>> createPurchase(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CreatePurchaseRequest request
    ) {
        PurchaseResponseDto created = purchaseService.createPurchase(
                request,
                userDetails.getId(),
                userDetails.getUsername()
        );
        return new ResponseEntity<>(ApiResponse.ok("Purchase recorded successfully", created), HttpStatus.CREATED);
    }

    @PostMapping("/{id}/payment")
    public ResponseEntity<ApiResponse<PurchaseResponseDto>> recordPayment(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody com.antigravity.billing.dto.payment.RecordPaymentRequest request
    ) {
        PurchaseResponseDto updated = purchaseService.recordPayment(
                id,
                request,
                userDetails.getId(),
                userDetails.getUsername()
        );
        return ResponseEntity.ok(ApiResponse.ok("Payment recorded successfully", updated));
    }
}
