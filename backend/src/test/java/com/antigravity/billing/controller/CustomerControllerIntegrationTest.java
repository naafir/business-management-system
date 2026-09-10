package com.antigravity.billing.controller;

import com.antigravity.billing.dto.auth.LoginRequest;
import com.antigravity.billing.dto.customer.CreateCustomerRequest;
import com.antigravity.billing.repository.UserRepository;
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

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class CustomerControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

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

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        authToken = root.path("data").path("token").asText();
    }

    @Test
    @DisplayName("Lists customers successfully")
    void testListCustomers() throws Exception {
        mockMvc.perform(get("/api/v1/customers")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", notNullValue()));
    }

    @Test
    @DisplayName("Creates B2C customer and toggles status")
    void testCreateB2cCustomerAndToggleStatus() throws Exception {
        CreateCustomerRequest request = new CreateCustomerRequest();
        request.setName("Aarav Patel " + System.currentTimeMillis());
        request.setPhone("9876543210");
        request.setStateName("Maharashtra");
        request.setStateCode("27");
        request.setCustomerType("B2C");
        request.setCreditLimit(new BigDecimal("25000.00"));

        MvcResult createResult = mockMvc.perform(post("/api/v1/customers")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.customerType", is("B2C")))
                .andReturn();

        JsonNode createdNode = objectMapper.readTree(createResult.getResponse().getContentAsString());
        String customerId = createdNode.path("data").path("id").asText();

        // Toggle status
        mockMvc.perform(patch("/api/v1/customers/" + customerId + "/status")
                        .param("active", "false")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active", is(false)));
    }

    @Test
    @DisplayName("Creates B2B customer with valid GSTIN")
    void testCreateB2bCustomerWithGstin() throws Exception {
        CreateCustomerRequest request = new CreateCustomerRequest();
        request.setName("Apex Tech Solutions " + System.currentTimeMillis());
        request.setBusinessName("Apex Tech");
        request.setGstin("27AABCA1234A1Z5");
        request.setStateName("Maharashtra");
        request.setStateCode("27");
        request.setCustomerType("B2B");
        request.setCreditLimit(new BigDecimal("500000.00"));

        mockMvc.perform(post("/api/v1/customers")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.customerType", is("B2B")))
                .andExpect(jsonPath("$.data.gstin", is("27AABCA1234A1Z5")));
    }
}
