package com.antigravity.billing.controller;

import com.antigravity.billing.dto.auth.LoginRequest;
import com.antigravity.billing.dto.inventory.StockAdjustmentMode;
import com.antigravity.billing.dto.inventory.StockAdjustmentRequest;
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
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class InventoryControllerIntegrationTest {

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
    @DisplayName("Retrieves inventory summary metrics successfully")
    void testGetSummary() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/summary")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.totalProductsCount", notNullValue()))
                .andExpect(jsonPath("$.data.totalValuation", notNullValue()));
    }

    @Test
    @DisplayName("Creates product and performs stock adjustment with ledger verification")
    void testAdjustStockAndVerifyLedger() throws Exception {
        // 1. Create a test product with initial opening stock
        String sku = "INV-TEST-" + System.currentTimeMillis();
        CreateProductRequest createProduct = new CreateProductRequest();
        createProduct.setSku(sku);
        createProduct.setName("Egg Hakka Noodles 500g");
        createProduct.setHsnSac("1902");
        createProduct.setUnit("PKT");
        createProduct.setPurchasePrice(new BigDecimal("45.00"));
        createProduct.setSellingPrice(new BigDecimal("70.00"));
        createProduct.setOpeningStock(new BigDecimal("100.00"));
        createProduct.setMinStockLevel(new BigDecimal("20.00"));

        MvcResult createProdResult = mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createProduct)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode prodNode = objectMapper.readTree(createProdResult.getResponse().getContentAsString());
        UUID productId = UUID.fromString(prodNode.path("data").path("id").asText());

        // 2. Perform Stock Adjustment (Increase +25)
        StockAdjustmentRequest adjustRequest = StockAdjustmentRequest.builder()
                .productId(productId)
                .adjustmentMode(StockAdjustmentMode.INCREASE)
                .quantity(new BigDecimal("25.00"))
                .reason("Fresh consignment received from mill")
                .notes("Batch #2026-N09")
                .build();

        mockMvc.perform(post("/api/v1/inventory/adjust")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adjustRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.previousStock", is(100.0)))
                .andExpect(jsonPath("$.data.newStock", is(125.0)))
                .andExpect(jsonPath("$.data.transactionType", is("ADJUSTMENT")));

        // 3. Verify ledger entries for this product
        mockMvc.perform(get("/api/v1/inventory/product/" + productId + "/history")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(greaterThanOrEqualTo(2))))
                .andExpect(jsonPath("$.data.content[0].newStock", is(125.0)));
    }

    @Test
    @DisplayName("Lists global ledger with pagination and filters")
    void testGetGlobalLedger() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/ledger")
                        .header("Authorization", "Bearer " + authToken)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", notNullValue()));
    }
}
