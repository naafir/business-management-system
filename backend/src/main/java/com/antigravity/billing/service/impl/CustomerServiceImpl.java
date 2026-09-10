package com.antigravity.billing.service.impl;

import com.antigravity.billing.dto.common.PageResponse;
import com.antigravity.billing.dto.customer.CreateCustomerRequest;
import com.antigravity.billing.dto.customer.CustomerResponseDto;
import com.antigravity.billing.dto.customer.UpdateCustomerRequest;
import com.antigravity.billing.entity.Customer;
import com.antigravity.billing.exception.ApiException;
import com.antigravity.billing.exception.ResourceNotFoundException;
import com.antigravity.billing.repository.CustomerRepository;
import com.antigravity.billing.service.AuditService;
import com.antigravity.billing.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final AuditService auditService;

    @Override
    @Transactional
    public CustomerResponseDto createCustomer(CreateCustomerRequest request, UUID userId, String username) {
        if ("B2B".equalsIgnoreCase(request.getCustomerType()) && (request.getGstin() == null || request.getGstin().trim().isEmpty())) {
            throw new ApiException("GSTIN is required for B2B registered customers", HttpStatus.BAD_REQUEST);
        }

        Customer customer = Customer.builder()
                .name(request.getName().trim())
                .businessName(request.getBusinessName() != null ? request.getBusinessName().trim() : null)
                .phone(request.getPhone())
                .email(request.getEmail())
                .billingAddress(request.getBillingAddress())
                .shippingAddress(request.getShippingAddress())
                .city(request.getCity())
                .stateName(request.getStateName().trim())
                .stateCode(request.getStateCode().trim())
                .pinCode(request.getPinCode())
                .gstin(request.getGstin() != null && !request.getGstin().trim().isEmpty() ? request.getGstin().trim().toUpperCase() : null)
                .customerType(request.getCustomerType().toUpperCase())
                .creditLimit(request.getCreditLimit())
                .notes(request.getNotes())
                .active(true)
                .build();

        Customer saved = customerRepository.save(customer);

        auditService.logAction(
                userId,
                username,
                "CREATE_CUSTOMER",
                "Customer",
                saved.getId().toString(),
                "Created customer: " + saved.getName() + " (" + saved.getCustomerType() + ")"
        );

        return mapToDto(saved);
    }

    @Override
    @Transactional
    public CustomerResponseDto updateCustomer(UUID id, UpdateCustomerRequest request, UUID userId, String username) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "id", id));

        customer.setName(request.getName().trim());
        customer.setBusinessName(request.getBusinessName() != null ? request.getBusinessName().trim() : null);
        customer.setPhone(request.getPhone());
        customer.setEmail(request.getEmail());
        customer.setBillingAddress(request.getBillingAddress());
        customer.setShippingAddress(request.getShippingAddress());
        customer.setCity(request.getCity());
        customer.setStateName(request.getStateName().trim());
        customer.setStateCode(request.getStateCode().trim());
        customer.setPinCode(request.getPinCode());
        customer.setGstin(request.getGstin() != null && !request.getGstin().trim().isEmpty() ? request.getGstin().trim().toUpperCase() : null);
        customer.setCustomerType(request.getCustomerType().toUpperCase());
        customer.setCreditLimit(request.getCreditLimit());
        customer.setNotes(request.getNotes());

        Customer updated = customerRepository.save(customer);

        auditService.logAction(
                userId,
                username,
                "UPDATE_CUSTOMER",
                "Customer",
                updated.getId().toString(),
                "Updated customer: " + updated.getName()
        );

        return mapToDto(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerResponseDto getCustomerById(UUID id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "id", id));
        return mapToDto(customer);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CustomerResponseDto> listCustomers(String search, String customerType, Boolean activeOnly, Pageable pageable) {
        String searchTerm = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        Page<Customer> page = customerRepository.findCustomersWithFilters(searchTerm, customerType, activeOnly, pageable);
        return PageResponse.of(page.map(this::mapToDto));
    }

    @Override
    @Transactional
    public CustomerResponseDto toggleCustomerStatus(UUID id, boolean active, UUID userId, String username) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "id", id));

        customer.setActive(active);
        Customer updated = customerRepository.save(customer);

        auditService.logAction(
                userId,
                username,
                active ? "ACTIVATE_CUSTOMER" : "DEACTIVATE_CUSTOMER",
                "Customer",
                updated.getId().toString(),
                (active ? "Activated" : "Deactivated") + " customer: " + updated.getName()
        );

        return mapToDto(updated);
    }

    private CustomerResponseDto mapToDto(Customer c) {
        return CustomerResponseDto.builder()
                .id(c.getId())
                .name(c.getName())
                .businessName(c.getBusinessName())
                .phone(c.getPhone())
                .email(c.getEmail())
                .billingAddress(c.getBillingAddress())
                .shippingAddress(c.getShippingAddress())
                .city(c.getCity())
                .stateName(c.getStateName())
                .stateCode(c.getStateCode())
                .pinCode(c.getPinCode())
                .gstin(c.getGstin())
                .customerType(c.getCustomerType())
                .creditLimit(c.getCreditLimit())
                .outstandingBalance(c.getOutstandingBalance())
                .notes(c.getNotes())
                .active(c.isActive())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }
}