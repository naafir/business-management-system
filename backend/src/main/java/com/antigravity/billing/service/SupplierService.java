package com.antigravity.billing.service;

import com.antigravity.billing.dto.common.PageResponse;
import com.antigravity.billing.dto.supplier.CreateSupplierRequest;
import com.antigravity.billing.dto.supplier.SupplierResponseDto;
import com.antigravity.billing.dto.supplier.UpdateSupplierRequest;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface SupplierService {
    SupplierResponseDto createSupplier(CreateSupplierRequest request, UUID userId, String username);
    SupplierResponseDto updateSupplier(UUID id, UpdateSupplierRequest request, UUID userId, String username);
    SupplierResponseDto getSupplierById(UUID id);
    PageResponse<SupplierResponseDto> listSuppliers(String search, Boolean activeOnly, Pageable pageable);
    SupplierResponseDto toggleSupplierStatus(UUID id, boolean active, UUID userId, String username);
}