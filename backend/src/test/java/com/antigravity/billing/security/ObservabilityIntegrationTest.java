package com.antigravity.billing.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ObservabilityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Request ID is generated and returned in headers when absent")
    void testRequestIdGeneration() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-ID"))
                .andExpect(header().string("X-Request-ID", notNullValue()));
    }

    @Test
    @DisplayName("Existing Request ID is preserved and returned")
    void testRequestIdPreservation() throws Exception {
        String existingId = "req-12345-abcde";
        mockMvc.perform(get("/api/v1/health").header("X-Request-ID", existingId))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-ID", existingId));
    }
}
