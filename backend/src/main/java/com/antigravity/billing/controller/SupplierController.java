package com.antigravity.billing.controller;

import com.antigravity.billing.dto.common.ApiResponse;
import com.antigravity.billing.dto.common.PageResponse;
import com.antigravity.billing.dto.supplier.CreateSupplierRequest;
import com.antigravity.billing.dto.supplier.SupplierResponseDto;
import com.antigravity.billing.dto.supplier.UpdateSupplierRequest;
import com.antigravity.billing.security.CustomUserDetails;
import com.antigravity.billing.service.SupplierService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/suppliers")
@RequiredArgsConstructor
public class SupplierController {

    private final SupplierService supplierService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<SupplierResponseDto>>> listSuppliers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean activeOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        PageResponse<SupplierResponseDto> response = supplierService.listSuppliers(search, activeOnly, pageable);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SupplierResponseDto>> getSupplier(@PathVariable UUID id) {
        SupplierResponseDto supplier = supplierService.getSupplierById(id);
        return ResponseEntity.ok(ApiResponse.ok(supplier));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SupplierResponseDto>> createSupplier(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CreateSupplierRequest request
    ) {
        SupplierResponseDto created = supplierService.createSupplier(
                request,
                userDetails.getId(),
                userDetails.getUsername()
        );
        return new ResponseEntity<>(ApiResponse.ok("Supplier created successfully", created), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SupplierResponseDto>> updateSupplier(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody UpdateSupplierRequest request
    ) {
        SupplierResponseDto updated = supplierService.updateSupplier(
                id,
                request,
                userDetails.getId(),
                userDetails.getUsername()
        );
        return ResponseEntity.ok(ApiResponse.ok("Supplier updated successfully", updated));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<SupplierResponseDto>> toggleStatus(
            @PathVariable UUID id,
            @RequestParam boolean active,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        SupplierResponseDto updated = supplierService.toggleSupplierStatus(
                id,
                active,
                userDetails.getId(),
                userDetails.getUsername()
        );
        return ResponseEntity.ok(ApiResponse.ok("Supplier status updated successfully", updated));
    }
}