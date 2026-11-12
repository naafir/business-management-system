package com.antigravity.billing.security;

import com.antigravity.billing.entity.User;
import com.antigravity.billing.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthorizationSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setupStaffUser() {
        if (!userRepository.existsByUsername("staff_user")) {
            User staff = User.builder()
                    .username("staff_user")
                    .email("staff@example.com")
                    .fullName("Staff User")
                    .passwordHash(passwordEncoder.encode("StaffPass123!"))
                    .role("ROLE_STAFF")
                    .active(true)
                    .build();
            userRepository.save(staff);
        }
    }

    @Test
    @DisplayName("Unauthenticated request to sensitive endpoint returns HTTP 401")
    void testUnauthenticatedUserEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.error", is("UNAUTHORIZED")));
    }

    @Test
    @WithMockUser(username = "staff_user", roles = {"STAFF"})
    @DisplayName("STAFF role attempting to access OWNER-only /users endpoint receives 403 Forbidden")
    void testStaffAccessToUserManagement() throws Exception {
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.error", is("ACCESS_DENIED")));
    }

    @Test
    @WithMockUser(username = "staff_user", roles = {"STAFF"})
    @DisplayName("STAFF role attempting to access /backups endpoint receives 403 Forbidden")
    void testStaffAccessToBackups() throws Exception {
        mockMvc.perform(get("/api/v1/backups"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.error", is("ACCESS_DENIED")));
    }

    @Test
    @WithMockUser(username = "staff_user", roles = {"STAFF"})
    @DisplayName("STAFF role attempting to access /audit endpoint receives 403 Forbidden")
    void testStaffAccessToAuditLogs() throws Exception {
        mockMvc.perform(get("/api/v1/audit"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.error", is("ACCESS_DENIED")));
    }

    @Test
    @WithMockUser(username = "owner_user", roles = {"OWNER"})
    @DisplayName("OWNER role accessing /users endpoint receives HTTP 200 OK")
    void testOwnerAccessToUserManagement() throws Exception {
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin_user", roles = {"ADMIN"})
    @DisplayName("ADMIN role accessing /backups endpoint receives HTTP 200 OK")
    void testAdminAccessToBackups() throws Exception {
        mockMvc.perform(get("/api/v1/backups"))
                .andExpect(status().isOk());
    }
}
