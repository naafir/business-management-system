package com.antigravity.billing.controller;

import com.antigravity.billing.dto.common.ApiResponse;
import com.antigravity.billing.dto.common.PageResponse;
import com.antigravity.billing.dto.customer.CreateCustomerRequest;
import com.antigravity.billing.dto.customer.CustomerResponseDto;
import com.antigravity.billing.dto.customer.UpdateCustomerRequest;
import com.antigravity.billing.security.CustomUserDetails;
import com.antigravity.billing.service.CustomerService;
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
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<CustomerResponseDto>>> listCustomers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String customerType,
            @RequestParam(required = false) Boolean activeOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        PageResponse<CustomerResponseDto> response = customerService.listCustomers(search, customerType, activeOnly, pageable);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerResponseDto>> getCustomer(@PathVariable UUID id) {
        CustomerResponseDto customer = customerService.getCustomerById(id);
        return ResponseEntity.ok(ApiResponse.ok(customer));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CustomerResponseDto>> createCustomer(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CreateCustomerRequest request
    ) {
        CustomerResponseDto created = customerService.createCustomer(
                request,
                userDetails.getId(),
                userDetails.getUsername()
        );
        return new ResponseEntity<>(ApiResponse.ok("Customer created successfully", created), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerResponseDto>> updateCustomer(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody UpdateCustomerRequest request
    ) {
        CustomerResponseDto updated = customerService.updateCustomer(
                id,
                request,
                userDetails.getId(),
                userDetails.getUsername()
        );
        return ResponseEntity.ok(ApiResponse.ok("Customer updated successfully", updated));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<CustomerResponseDto>> toggleStatus(
            @PathVariable UUID id,
            @RequestParam boolean active,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        CustomerResponseDto updated = customerService.toggleCustomerStatus(
                id,
                active,
                userDetails.getId(),
                userDetails.getUsername()
        );
        return ResponseEntity.ok(ApiResponse.ok("Customer status updated successfully", updated));
    }
}