package com.antigravity.billing.controller;

import com.antigravity.billing.dto.common.ApiResponse;
import com.antigravity.billing.dto.common.PageResponse;
import com.antigravity.billing.dto.sale.CreateSaleRequest;
import com.antigravity.billing.dto.sale.SaleResponseDto;
import com.antigravity.billing.dto.sale.SaleSummaryDto;
import com.antigravity.billing.entity.PaymentStatus;
import com.antigravity.billing.security.CustomUserDetails;
import com.antigravity.billing.service.SaleService;
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
@RequestMapping("/api/v1/sales")
@RequiredArgsConstructor
public class SaleController {

    private final SaleService saleService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<SaleResponseDto>>> listSales(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "saleDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        PageResponse<SaleResponseDto> response = saleService.listSales(search, customerId, status, startDate, endDate, pageable);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<SaleSummaryDto>> getSaleSummary() {
        SaleSummaryDto summary = saleService.getSaleSummary();
        return ResponseEntity.ok(ApiResponse.ok(summary));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SaleResponseDto>> getSaleById(@PathVariable UUID id) {
        SaleResponseDto sale = saleService.getSaleById(id);
        return ResponseEntity.ok(ApiResponse.ok(sale));
    }

    @GetMapping("/number/{saleNumber}")
    public ResponseEntity<ApiResponse<SaleResponseDto>> getSaleByNumber(@PathVariable String saleNumber) {
        SaleResponseDto sale = saleService.getSaleByNumber(saleNumber);
        return ResponseEntity.ok(ApiResponse.ok(sale));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SaleResponseDto>> createSale(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CreateSaleRequest request
    ) {
        SaleResponseDto created = saleService.createSale(
                request,
                userDetails.getId(),
                userDetails.getUsername()
        );
        return new ResponseEntity<>(ApiResponse.ok("Sale recorded successfully", created), HttpStatus.CREATED);
    }
}
