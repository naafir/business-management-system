package com.antigravity.billing.service;

import com.antigravity.billing.dto.common.PageResponse;
import com.antigravity.billing.dto.customer.CreateCustomerRequest;
import com.antigravity.billing.dto.customer.CustomerResponseDto;
import com.antigravity.billing.dto.customer.UpdateCustomerRequest;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface CustomerService {
    CustomerResponseDto createCustomer(CreateCustomerRequest request, UUID userId, String username);
    CustomerResponseDto updateCustomer(UUID id, UpdateCustomerRequest request, UUID userId, String username);
    CustomerResponseDto getCustomerById(UUID id);
    PageResponse<CustomerResponseDto> listCustomers(String search, String customerType, Boolean activeOnly, Pageable pageable);
    CustomerResponseDto toggleCustomerStatus(UUID id, boolean active, UUID userId, String username);
}