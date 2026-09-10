package com.antigravity.billing.service;

import com.antigravity.billing.dto.customer.CreateCustomerRequest;
import com.antigravity.billing.dto.customer.CustomerResponseDto;
import com.antigravity.billing.entity.Customer;
import com.antigravity.billing.exception.ApiException;
import com.antigravity.billing.repository.CustomerRepository;
import com.antigravity.billing.service.impl.CustomerServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private CustomerServiceImpl customerService;

    @Test
    @DisplayName("Successfully creates B2C customer without GSTIN")
    void testCreateB2cCustomer() {
        CreateCustomerRequest req = new CreateCustomerRequest();
        req.setName("Rahul Sharma");
        req.setStateName("Maharashtra");
        req.setStateCode("27");
        req.setCustomerType("B2C");
        req.setCreditLimit(new BigDecimal("50000.00"));

        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> {
            Customer c = invocation.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });

        UUID userId = UUID.randomUUID();
        CustomerResponseDto result = customerService.createCustomer(req, userId, "admin");

        assertThat(result.getName()).isEqualTo("Rahul Sharma");
        assertThat(result.getCustomerType()).isEqualTo("B2C");
        assertThat(result.getGstin()).isNull();
        verify(auditService, times(1)).logAction(eq(userId), eq("admin"), eq("CREATE_CUSTOMER"), any(), any(), any());
    }

    @Test
    @DisplayName("Rejects B2B customer if GSTIN is missing")
    void testB2bRequiresGstin() {
        CreateCustomerRequest req = new CreateCustomerRequest();
        req.setName("Tech Corp");
        req.setStateName("Karnataka");
        req.setStateCode("29");
        req.setCustomerType("B2B");
        req.setGstin("");

        assertThatThrownBy(() -> customerService.createCustomer(req, UUID.randomUUID(), "admin"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("GSTIN is required for B2B registered customers");
    }

    @Test
    @DisplayName("Successfully updates existing customer")
    void testUpdateCustomer() {
        UUID id = UUID.randomUUID();
        Customer customer = Customer.builder()
                .name("Old Customer Name")
                .stateName("Maharashtra")
                .stateCode("27")
                .customerType("B2C")
                .active(true)
                .build();
        customer.setId(id);

        com.antigravity.billing.dto.customer.UpdateCustomerRequest updateReq = new com.antigravity.billing.dto.customer.UpdateCustomerRequest();
        updateReq.setName("Updated Customer Name");
        updateReq.setStateName("Gujarat");
        updateReq.setStateCode("24");
        updateReq.setCustomerType("B2C");
        updateReq.setCreditLimit(new BigDecimal("100000.00"));

        when(customerRepository.findById(id)).thenReturn(java.util.Optional.of(customer));
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));

        UUID userId = UUID.randomUUID();
        CustomerResponseDto result = customerService.updateCustomer(id, updateReq, userId, "admin");

        assertThat(result.getName()).isEqualTo("Updated Customer Name");
        assertThat(result.getStateCode()).isEqualTo("24");
        verify(auditService, times(1)).logAction(eq(userId), eq("admin"), eq("UPDATE_CUSTOMER"), any(), any(), any());
    }

    @Test
    @DisplayName("Successfully toggles customer status")
    void testToggleCustomerStatus() {
        UUID id = UUID.randomUUID();
        Customer customer = Customer.builder()
                .name("Active Customer")
                .stateName("Maharashtra")
                .stateCode("27")
                .active(true)
                .build();
        customer.setId(id);

        when(customerRepository.findById(id)).thenReturn(java.util.Optional.of(customer));
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));

        UUID userId = UUID.randomUUID();
        CustomerResponseDto result = customerService.toggleCustomerStatus(id, false, userId, "admin");

        assertThat(result.isActive()).isFalse();
        verify(auditService, times(1)).logAction(eq(userId), eq("admin"), eq("DEACTIVATE_CUSTOMER"), any(), any(), any());
    }
}