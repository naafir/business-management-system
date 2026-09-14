package com.antigravity.billing.controller;

import com.antigravity.billing.dto.auth.LoginRequest;
import com.antigravity.billing.dto.supplier.CreateSupplierRequest;
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

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SupplierControllerIntegrationTest {

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
    @DisplayName("Lists suppliers successfully")
    void testListSuppliers() throws Exception {
        mockMvc.perform(get("/api/v1/suppliers")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", notNullValue()));
    }

    @Test
    @DisplayName("Creates supplier, retrieves, and toggles status")
    void testCreateAndToggleSupplier() throws Exception {
        CreateSupplierRequest request = new CreateSupplierRequest();
        request.setName("Global Metalworks " + System.currentTimeMillis());
        request.setBusinessName("Global Metalworks LLP");
        request.setPhone("9811223344");
        request.setEmail("sales@globalmetal.com");
        request.setStateName("Haryana");
        request.setStateCode("06");
        request.setGstin("06AABCG9876K1Z1");

        MvcResult createResult = mockMvc.perform(post("/api/v1/suppliers")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name", containsString("Global Metalworks")))
                .andReturn();

        JsonNode createdNode = objectMapper.readTree(createResult.getResponse().getContentAsString());
        String supplierId = createdNode.path("data").path("id").asText();

        // Retrieve by ID
        mockMvc.perform(get("/api/v1/suppliers/" + supplierId)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stateCode", is("06")));

        // Toggle status
        mockMvc.perform(patch("/api/v1/suppliers/" + supplierId + "/status")
                        .param("active", "false")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active", is(false)));
    }
}
