package com.antigravity.billing.security;

import com.antigravity.billing.dto.inventory.StockAdjustmentRequest;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RbacSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // --- A. Protected Settings Endpoint ---

    @Test
    @WithMockUser(username = "staff_user", roles = {"STAFF"})
    @DisplayName("A. STAFF attempting settings profile -> 403 Forbidden")
    void testStaffSettingsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/settings/profile"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "owner_user", roles = {"OWNER"})
    @DisplayName("F. OWNER attempting settings profile -> 200 OK")
    void testOwnerSettingsAllowed() throws Exception {
        mockMvc.perform(get("/api/v1/settings/profile"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin_user", roles = {"ADMIN"})
    @DisplayName("G. ADMIN attempting settings profile -> 200 OK")
    void testAdminSettingsAllowed() throws Exception {
        mockMvc.perform(get("/api/v1/settings/profile"))
                .andExpect(status().isOk());
    }

    // --- B. Manual Inventory Adjustments ---

    @Test
    @WithMockUser(username = "staff_user", roles = {"STAFF"})
    @DisplayName("B. STAFF attempting stock adjustment -> 403 Forbidden")
    void testStaffStockAdjustmentForbidden() throws Exception {
        StockAdjustmentRequest request = new StockAdjustmentRequest();
        request.setProductId(UUID.randomUUID());
        request.setAdjustmentMode(com.antigravity.billing.dto.inventory.StockAdjustmentMode.INCREASE);
        request.setQuantity(new BigDecimal("10.00"));
        request.setReason("CORRECTION");

        mockMvc.perform(post("/api/v1/inventory/adjust")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("G. ADMIN attempting stock adjustment -> 404 or 400 (Not 403 Forbidden)")
    void testAdminStockAdjustmentAuthorized() throws Exception {
        StockAdjustmentRequest request = new StockAdjustmentRequest();
        request.setProductId(UUID.randomUUID());
        request.setAdjustmentMode(com.antigravity.billing.dto.inventory.StockAdjustmentMode.INCREASE);
        request.setQuantity(new BigDecimal("10.00"));
        request.setReason("CORRECTION");

        org.springframework.security.core.userdetails.UserDetails adminUser = new com.antigravity.billing.security.CustomUserDetails(
                UUID.randomUUID(), "admin_user", "password", "admin@example.com", "ADMIN", true, java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN")));

        // Fails business logic due to mock user context (500/404), but passes security check (is not 403 Forbidden)
        mockMvc.perform(post("/api/v1/inventory/adjust")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(adminUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotEqualTo(403));
    }

    // --- C. Financial Reports ---

    @Test
    @WithMockUser(username = "staff_user", roles = {"STAFF"})
    @DisplayName("C. STAFF attempting GST output report -> 403 Forbidden")
    void testStaffFinancialReportForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/reports/gst-output")
                        .param("startDate", "2026-01-01")
                        .param("endDate", "2026-01-31"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin_user", roles = {"ADMIN"})
    @DisplayName("G. ADMIN attempting GST output report -> 200 OK")
    void testAdminFinancialReportAllowed() throws Exception {
        mockMvc.perform(get("/api/v1/reports/gst-output")
                        .param("startDate", "2026-01-01")
                        .param("endDate", "2026-01-31"))
                .andExpect(status().isOk());
    }

    // --- D. Audit Logs ---

    @Test
    @WithMockUser(username = "staff_user", roles = {"STAFF"})
    @DisplayName("D. STAFF attempting audit log viewing -> 403 Forbidden")
    void testStaffAuditLogsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/audit"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "owner_user", roles = {"OWNER"})
    @DisplayName("F. OWNER attempting audit log viewing -> 200 OK")
    void testOwnerAuditLogsAllowed() throws Exception {
        mockMvc.perform(get("/api/v1/audit"))
                .andExpect(status().isOk());
    }

    // --- E. Expense Deletion ---

    @Test
    @WithMockUser(username = "staff_user", roles = {"STAFF"})
    @DisplayName("E. STAFF attempting expense deletion -> 403 Forbidden")
    void testStaffExpenseDeletionForbidden() throws Exception {
        mockMvc.perform(delete("/api/v1/expenses/" + UUID.randomUUID()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "staff_user", roles = {"STAFF"})
    @DisplayName("STAFF attempting expense listing -> 200 OK (Cashier operational access preserved)")
    void testStaffExpenseListingAllowed() throws Exception {
        mockMvc.perform(get("/api/v1/expenses"))
                .andExpect(status().isOk());
    }

    // --- H. User Management Restrictions ---

    @Test
    @WithMockUser(username = "staff_user", roles = {"STAFF"})
    @DisplayName("H. STAFF attempting user management -> 403 Forbidden")
    void testStaffUserManagementForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin_user", roles = {"ADMIN"})
    @DisplayName("H. ADMIN attempting user management -> 403 Forbidden (OWNER only restriction preserved)")
    void testAdminUserManagementForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "owner_user", roles = {"OWNER"})
    @DisplayName("H. OWNER attempting user management -> 200 OK")
    void testOwnerUserManagementAllowed() throws Exception {
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isOk());
    }

    // --- I. Backup Restrictions ---

    @Test
    @WithMockUser(username = "staff_user", roles = {"STAFF"})
    @DisplayName("I. STAFF attempting backup listing -> 403 Forbidden")
    void testStaffBackupForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/backups"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin_user", roles = {"ADMIN"})
    @DisplayName("I. ADMIN attempting backup listing -> 200 OK")
    void testAdminBackupAllowed() throws Exception {
        mockMvc.perform(get("/api/v1/backups"))
                .andExpect(status().isOk());
    }

    // --- Cashier/Staff Normal Operations Preserved ---

    @Test
    @WithMockUser(username = "staff_user", roles = {"STAFF"})
    @DisplayName("Normal operations: STAFF viewing products -> 200 OK")
    void testStaffProductAccessAllowed() throws Exception {
        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "staff_user", roles = {"STAFF"})
    @DisplayName("Normal operations: STAFF viewing sales -> 200 OK")
    void testStaffSalesAccessAllowed() throws Exception {
        mockMvc.perform(get("/api/v1/sales"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "staff_user", roles = {"STAFF"})
    @DisplayName("Normal operations: STAFF viewing customers -> 200 OK")
    void testStaffCustomerAccessAllowed() throws Exception {
        mockMvc.perform(get("/api/v1/customers"))
                .andExpect(status().isOk());
    }
}
