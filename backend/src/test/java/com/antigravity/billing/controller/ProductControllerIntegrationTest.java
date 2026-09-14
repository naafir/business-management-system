package com.antigravity.billing.controller;

import com.antigravity.billing.dto.auth.LoginRequest;
import com.antigravity.billing.dto.product.CreateProductCategoryRequest;
import com.antigravity.billing.dto.product.CreateProductRequest;
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
@ActiveProfiles("test")
class ProductControllerIntegrationTest {

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
    @DisplayName("Lists products successfully")
    void testListProducts() throws Exception {
        mockMvc.perform(get("/api/v1/products")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", notNullValue()));
    }

    @Test
    @DisplayName("Creates product category via /api/v1/categories")
    void testCreateCategory() throws Exception {
        CreateProductCategoryRequest request = new CreateProductCategoryRequest();
        request.setName("Test Accessories " + System.currentTimeMillis());
        request.setDescription("Test category description");

        mockMvc.perform(post("/api/v1/categories")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.name", containsString("Test Accessories")));
    }

    @Test
    @DisplayName("Creates, retrieves, and toggles product status")
    void testCreateAndToggleProduct() throws Exception {
        String sku = "SKU-TEST-" + System.currentTimeMillis();
        CreateProductRequest request = new CreateProductRequest();
        request.setSku(sku);
        request.setName("Test Keyboard");
        request.setHsnSac("8471");
        request.setUnit("PCS");
        request.setPurchasePrice(new BigDecimal("500.00"));
        request.setSellingPrice(new BigDecimal("750.00"));
        request.setOpeningStock(new BigDecimal("20.00"));
        request.setMinStockLevel(new BigDecimal("5.00"));

        MvcResult createResult = mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.sku", is(sku)))
                .andExpect(jsonPath("$.data.active", is(true)))
                .andReturn();

        JsonNode createdNode = objectMapper.readTree(createResult.getResponse().getContentAsString());
        String productId = createdNode.path("data").path("id").asText();

        // Retrieve by ID
        mockMvc.perform(get("/api/v1/products/" + productId)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name", is("Test Keyboard")));

        // Toggle status
        mockMvc.perform(patch("/api/v1/products/" + productId + "/status")
                        .param("active", "false")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active", is(false)));
    }
}
