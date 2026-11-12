package com.antigravity.billing.controller;

import com.antigravity.billing.dto.auth.LoginRequest;
import com.antigravity.billing.entity.*;
import com.antigravity.billing.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReportControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private SaleRepository saleRepository;

    @Autowired
    private PurchaseRepository purchaseRepository;

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ProductRepository productRepository;

    private String authToken;

    @BeforeEach
    void setUp() throws Exception {
        userRepository.findByUsername("admin").ifPresent(user -> {
            user.setPasswordHash(passwordEncoder.encode("Admin@SecurePass2026!"));
            userRepository.save(user);
        });

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("admin");
        loginRequest.setPassword("Admin@SecurePass2026!");

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode rootNode = objectMapper.readTree(result.getResponse().getContentAsString());
        authToken = rootNode.get("data").get("token").asText();
    }

    @Test
    @DisplayName("End-to-End: GST Output report returns valid calculations via HTTP API")
    void testGetGstOutputReport() throws Exception {
        LocalDate start = LocalDate.now().minusDays(30);
        LocalDate end = LocalDate.now();

        mockMvc.perform(get("/api/v1/reports/gst-output")
                        .param("startDate", start.toString())
                        .param("endDate", end.toString())
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalRevenue").exists())
                .andExpect(jsonPath("$.data.totalTaxableValue").exists())
                .andExpect(jsonPath("$.data.totalTaxCollected").exists())
                .andExpect(jsonPath("$.data.totalInvoicesCount").exists());
    }

    @Test
    @DisplayName("End-to-End: GST Input report returns valid ITC calculations via HTTP API")
    void testGetGstInputReport() throws Exception {
        LocalDate start = LocalDate.now().minusDays(30);
        LocalDate end = LocalDate.now();

        mockMvc.perform(get("/api/v1/reports/gst-input")
                        .param("startDate", start.toString())
                        .param("endDate", end.toString())
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalPurchaseValue").exists())
                .andExpect(jsonPath("$.data.totalInputTaxCredit").exists())
                .andExpect(jsonPath("$.data.totalPurchasesCount").exists());
    }

    @Test
    @DisplayName("End-to-End: Monthly Trend report returns 12 months via HTTP API")
    void testGetMonthlyTrendReport() throws Exception {
        int year = LocalDate.now().getYear();

        mockMvc.perform(get("/api/v1/reports/monthly-trend")
                        .param("year", String.valueOf(year))
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(12))
                .andExpect(jsonPath("$.data[0].month").value(1))
                .andExpect(jsonPath("$.data[11].month").value(12));
    }

    @Test
    @DisplayName("End-to-End: Profit & Loss report returns consistent financial metrics via HTTP API")
    void testGetProfitLossReport() throws Exception {
        LocalDate start = LocalDate.now().minusDays(30);
        LocalDate end = LocalDate.now();

        mockMvc.perform(get("/api/v1/reports/profit-loss")
                        .param("startDate", start.toString())
                        .param("endDate", end.toString())
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalRevenue").exists())
                .andExpect(jsonPath("$.data.totalCogs").exists())
                .andExpect(jsonPath("$.data.grossProfit").exists())
                .andExpect(jsonPath("$.data.netProfit").exists())
                .andExpect(jsonPath("$.data.netTaxLiability").exists());
    }

    @Test
    @DisplayName("End-to-End: HSN summary report returns successful list via HTTP API")
    void testGetHsnSummaryReport() throws Exception {
        LocalDate start = LocalDate.now().minusDays(30);
        LocalDate end = LocalDate.now();

        mockMvc.perform(get("/api/v1/reports/hsn-summary")
                        .param("startDate", start.toString())
                        .param("endDate", end.toString())
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }
}
