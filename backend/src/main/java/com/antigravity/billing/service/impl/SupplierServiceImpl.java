package com.antigravity.billing.service.impl;

import com.antigravity.billing.dto.common.PageResponse;
import com.antigravity.billing.dto.supplier.CreateSupplierRequest;
import com.antigravity.billing.dto.supplier.SupplierResponseDto;
import com.antigravity.billing.dto.supplier.UpdateSupplierRequest;
import com.antigravity.billing.entity.Supplier;
import com.antigravity.billing.exception.ResourceNotFoundException;
import com.antigravity.billing.repository.SupplierRepository;
import com.antigravity.billing.service.AuditService;
import com.antigravity.billing.service.SupplierService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SupplierServiceImpl implements SupplierService {

    private final SupplierRepository supplierRepository;
    private final AuditService auditService;

    @Override
    @Transactional
    public SupplierResponseDto createSupplier(CreateSupplierRequest request, UUID userId, String username) {
        Supplier supplier = Supplier.builder()
                .name(request.getName().trim())
                .businessName(request.getBusinessName() != null ? request.getBusinessName().trim() : null)
                .phone(request.getPhone())
                .email(request.getEmail())
                .address(request.getAddress())
                .city(request.getCity())
                .stateName(request.getStateName().trim())
                .stateCode(request.getStateCode().trim())
                .pinCode(request.getPinCode())
                .gstin(request.getGstin() != null && !request.getGstin().trim().isEmpty() ? request.getGstin().trim().toUpperCase() : null)
                .notes(request.getNotes())
                .active(true)
                .build();

        Supplier saved = supplierRepository.save(supplier);

        auditService.logAction(
                userId,
                username,
                "CREATE_SUPPLIER",
                "Supplier",
                saved.getId().toString(),
                "Created supplier: " + saved.getName()
        );

        return mapToDto(saved);
    }

    @Override
    @Transactional
    public SupplierResponseDto updateSupplier(UUID id, UpdateSupplierRequest request, UUID userId, String username) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier", "id", id));

        supplier.setName(request.getName().trim());
        supplier.setBusinessName(request.getBusinessName() != null ? request.getBusinessName().trim() : null);
        supplier.setPhone(request.getPhone());
        supplier.setEmail(request.getEmail());
        supplier.setAddress(request.getAddress());
        supplier.setCity(request.getCity());
        supplier.setStateName(request.getStateName().trim());
        supplier.setStateCode(request.getStateCode().trim());
        supplier.setPinCode(request.getPinCode());
        supplier.setGstin(request.getGstin() != null && !request.getGstin().trim().isEmpty() ? request.getGstin().trim().toUpperCase() : null);
        supplier.setNotes(request.getNotes());

        Supplier updated = supplierRepository.save(supplier);

        auditService.logAction(
                userId,
                username,
                "UPDATE_SUPPLIER",
                "Supplier",
                updated.getId().toString(),
                "Updated supplier: " + updated.getName()
        );

        return mapToDto(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public SupplierResponseDto getSupplierById(UUID id) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier", "id", id));
        return mapToDto(supplier);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<SupplierResponseDto> listSuppliers(String search, Boolean activeOnly, Pageable pageable) {
        String searchTerm = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        Page<Supplier> page = supplierRepository.findSuppliersWithFilters(searchTerm, activeOnly, pageable);
        return PageResponse.of(page.map(this::mapToDto));
    }

    @Override
    @Transactional
    public SupplierResponseDto toggleSupplierStatus(UUID id, boolean active, UUID userId, String username) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier", "id", id));

        supplier.setActive(active);
        Supplier updated = supplierRepository.save(supplier);

        auditService.logAction(
                userId,
                username,
                active ? "ACTIVATE_SUPPLIER" : "DEACTIVATE_SUPPLIER",
                "Supplier",
                updated.getId().toString(),
                (active ? "Activated" : "Deactivated") + " supplier: " + updated.getName()
        );

        return mapToDto(updated);
    }

    private SupplierResponseDto mapToDto(Supplier s) {
        return SupplierResponseDto.builder()
                .id(s.getId())
                .name(s.getName())
                .businessName(s.getBusinessName())
                .phone(s.getPhone())
                .email(s.getEmail())
                .address(s.getAddress())
                .city(s.getCity())
                .stateName(s.getStateName())
                .stateCode(s.getStateCode())
                .pinCode(s.getPinCode())
                .gstin(s.getGstin())
                .outstandingBalance(s.getOutstandingBalance())
                .notes(s.getNotes())
                .active(s.isActive())
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .build();
    }
}