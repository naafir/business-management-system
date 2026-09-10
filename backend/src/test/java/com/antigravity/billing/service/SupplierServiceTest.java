package com.antigravity.billing.service;

import com.antigravity.billing.dto.common.PageResponse;
import com.antigravity.billing.dto.supplier.CreateSupplierRequest;
import com.antigravity.billing.dto.supplier.SupplierResponseDto;
import com.antigravity.billing.dto.supplier.UpdateSupplierRequest;
import com.antigravity.billing.entity.Supplier;
import com.antigravity.billing.exception.ResourceNotFoundException;
import com.antigravity.billing.repository.SupplierRepository;
import com.antigravity.billing.service.impl.SupplierServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SupplierServiceTest {

    @Mock
    private SupplierRepository supplierRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private SupplierServiceImpl supplierService;

    @Test
    @DisplayName("Successfully creates supplier with valid data")
    void testCreateSupplierSuccess() {
        CreateSupplierRequest request = new CreateSupplierRequest();
        request.setName("National Hardware Supplies");
        request.setBusinessName("National Traders");
        request.setPhone("9876543210");
        request.setEmail("vendor@national.com");
        request.setAddress("Industrial Area, Phase 1");
        request.setCity("Pune");
        request.setStateName("Maharashtra");
        request.setStateCode("27");
        request.setGstin("27AAAAA0000A1Z5");

        when(supplierRepository.save(any(Supplier.class))).thenAnswer(inv -> {
            Supplier s = inv.getArgument(0);
            s.setId(UUID.randomUUID());
            s.setOutstandingBalance(BigDecimal.ZERO);
            s.setCreatedAt(java.time.Instant.now());
            s.setUpdatedAt(java.time.Instant.now());
            return s;
        });

        UUID userId = UUID.randomUUID();
        SupplierResponseDto result = supplierService.createSupplier(request, userId, "admin");

        assertThat(result.getName()).isEqualTo("National Hardware Supplies");
        assertThat(result.getStateCode()).isEqualTo("27");
        assertThat(result.getGstin()).isEqualTo("27AAAAA0000A1Z5");
        assertThat(result.isActive()).isTrue();
        verify(auditService, times(1)).logAction(eq(userId), eq("admin"), eq("CREATE_SUPPLIER"), any(), any(), any());
    }

    @Test
    @DisplayName("Successfully updates existing supplier")
    void testUpdateSupplierSuccess() {
        UUID id = UUID.randomUUID();
        Supplier existing = Supplier.builder()
                .name("Old Supplier Name")
                .stateName("Maharashtra")
                .stateCode("27")
                .active(true)
                .build();
        existing.setId(id);

        UpdateSupplierRequest updateReq = new UpdateSupplierRequest();
        updateReq.setName("New Supplier Name");
        updateReq.setStateName("Karnataka");
        updateReq.setStateCode("29");
        updateReq.setPhone("9123456780");

        when(supplierRepository.findById(id)).thenReturn(Optional.of(existing));
        when(supplierRepository.save(any(Supplier.class))).thenAnswer(inv -> inv.getArgument(0));

        UUID userId = UUID.randomUUID();
        SupplierResponseDto updated = supplierService.updateSupplier(id, updateReq, userId, "admin");

        assertThat(updated.getName()).isEqualTo("New Supplier Name");
        assertThat(updated.getStateCode()).isEqualTo("29");
        verify(auditService, times(1)).logAction(eq(userId), eq("admin"), eq("UPDATE_SUPPLIER"), any(), any(), any());
    }

    @Test
    @DisplayName("Successfully toggles supplier active status")
    void testToggleSupplierStatus() {
        UUID id = UUID.randomUUID();
        Supplier supplier = Supplier.builder()
                .name("Supplier Co")
                .stateName("Delhi")
                .stateCode("07")
                .active(true)
                .build();
        supplier.setId(id);

        when(supplierRepository.findById(id)).thenReturn(Optional.of(supplier));
        when(supplierRepository.save(any(Supplier.class))).thenAnswer(inv -> inv.getArgument(0));

        UUID userId = UUID.randomUUID();
        SupplierResponseDto result = supplierService.toggleSupplierStatus(id, false, userId, "admin");

        assertThat(result.isActive()).isFalse();
        verify(auditService, times(1)).logAction(eq(userId), eq("admin"), eq("DEACTIVATE_SUPPLIER"), any(), any(), any());
    }

    @Test
    @DisplayName("Throws ResourceNotFoundException when supplier not found")
    void testSupplierNotFound() {
        UUID id = UUID.randomUUID();
        when(supplierRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> supplierService.getSupplierById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Lists suppliers with pagination")
    void testListSuppliers() {
        Supplier s = Supplier.builder()
                .name("Vendor Corp")
                .stateName("Maharashtra")
                .stateCode("27")
                .active(true)
                .build();
        s.setId(UUID.randomUUID());

        Pageable pageable = PageRequest.of(0, 10);
        when(supplierRepository.findSuppliersWithFilters(eq("Vendor"), eq(true), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(s), pageable, 1));

        PageResponse<SupplierResponseDto> response = supplierService.listSuppliers("Vendor", true, pageable);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getName()).isEqualTo("Vendor Corp");
    }
}
