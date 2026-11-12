package com.antigravity.billing.validation;

import com.antigravity.billing.dto.customer.CreateCustomerRequest;
import com.antigravity.billing.dto.expense.CreateExpenseRequest;
import com.antigravity.billing.dto.payment.RecordPaymentRequest;
import com.antigravity.billing.dto.product.CreateProductRequest;
import com.antigravity.billing.dto.sale.CreateSaleRequest;
import com.antigravity.billing.dto.sale.SaleItemRequest;
import com.antigravity.billing.entity.PaymentMethod;
import com.antigravity.billing.repository.CustomerRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(username = "admin", roles = {"OWNER", "ADMIN"})
class ApiValidationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CustomerRepository customerRepository;

    @Test
    @DisplayName("Invalid or missing required fields return 400 Bad Request with fieldErrors")
    void testMissingRequiredFields() throws Exception {
        CreateCustomerRequest request = new CreateCustomerRequest();
        // Missing name, stateName, stateCode, customerType

        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("VALIDATION_FAILED")))
                .andExpect(jsonPath("$.fieldErrors.name", notNullValue()));
    }

    @Test
    @DisplayName("Negative financial amount returns 400 Bad Request")
    void testNegativeFinancialAmount() throws Exception {
        CreateExpenseRequest request = CreateExpenseRequest.builder()
                .expenseDate(LocalDate.now())
                .description("Test Expense")
                .amount(new BigDecimal("-100.00"))
                .build();

        mockMvc.perform(post("/api/v1/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.fieldErrors.amount", notNullValue()));
    }

    @Test
    @DisplayName("Zero quantity in sale item returns 400 Bad Request")
    void testZeroQuantityInSaleItem() throws Exception {
        SaleItemRequest item = SaleItemRequest.builder()
                .productId(UUID.randomUUID())
                .quantity(BigDecimal.ZERO)
                .unitPrice(new BigDecimal("100.00"))
                .build();

        CreateSaleRequest saleRequest = CreateSaleRequest.builder()
                .saleDate(LocalDate.now())
                .items(List.of(item))
                .build();

        mockMvc.perform(post("/api/v1/sales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(saleRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("VALIDATION_FAILED")));
    }

    @Test
    @DisplayName("Negative quantity in sale item returns 400 Bad Request")
    void testNegativeQuantityInSaleItem() throws Exception {
        SaleItemRequest item = SaleItemRequest.builder()
                .productId(UUID.randomUUID())
                .quantity(new BigDecimal("-5.00"))
                .unitPrice(new BigDecimal("100.00"))
                .build();

        CreateSaleRequest saleRequest = CreateSaleRequest.builder()
                .saleDate(LocalDate.now())
                .items(List.of(item))
                .build();

        mockMvc.perform(post("/api/v1/sales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(saleRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)));
    }

    @Test
    @DisplayName("Malformed UUID path variable returns controlled 400 Bad Request")
    void testMalformedUuidPathVariable() throws Exception {
        mockMvc.perform(get("/api/v1/products/not-a-valid-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("INVALID_ARGUMENT")))
                .andExpect(jsonPath("$.message", containsString("must be of type UUID")));
    }

    @Test
    @DisplayName("Invalid report date range (startDate after endDate) returns 400 Bad Request")
    void testInvalidReportDateRange() throws Exception {
        mockMvc.perform(get("/api/v1/reports/gst-output")
                        .param("startDate", "2026-12-31")
                        .param("endDate", "2026-01-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", containsString("Start date cannot be after end date")));
    }

    @Test
    @DisplayName("Malformed email format returns 400 Bad Request")
    void testMalformedEmail() throws Exception {
        CreateCustomerRequest request = new CreateCustomerRequest();
        request.setName("Valid Name");
        request.setStateName("Maharashtra");
        request.setStateCode("27");
        request.setCustomerType("B2C");
        request.setEmail("not-an-email-address");

        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)));
    }

    @Test
    @DisplayName("Excessively long input string returns 400 Bad Request")
    void testExcessivelyLongInput() throws Exception {
        CreateCustomerRequest request = new CreateCustomerRequest();
        request.setName("A".repeat(250)); // max limit is 200
        request.setStateName("Maharashtra");
        request.setStateCode("27");
        request.setCustomerType("B2C");

        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.fieldErrors.name", notNullValue()));
    }

    @Test
    @DisplayName("Empty sale line items returns 400 Bad Request")
    void testEmptySaleLineItems() throws Exception {
        CreateSaleRequest saleRequest = CreateSaleRequest.builder()
                .saleDate(LocalDate.now())
                .items(Collections.emptyList())
                .build();

        mockMvc.perform(post("/api/v1/sales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(saleRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.fieldErrors.items", notNullValue()));
    }

    @Test
    @DisplayName("Error response contains no stack traces, SQL, or internal class names")
    void testErrorResponseNoStackTraces() throws Exception {
        mockMvc.perform(get("/api/v1/products/" + UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.stackTrace").doesNotExist())
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.sql").doesNotExist());
    }
}
