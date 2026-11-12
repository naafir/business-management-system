package com.antigravity.billing.security;

import com.antigravity.billing.dto.auth.CreateUserRequest;
import com.antigravity.billing.repository.UserRepository;
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

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserManagementSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Test
    @WithMockUser(username = "staff_user", roles = {"STAFF"})
    @DisplayName("STAFF user cannot create new user accounts")
    void testStaffCannotCreateUser() throws Exception {
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("new_staff");
        request.setEmail("newstaff@example.com");
        request.setFullName("New Staff");
        request.setPassword("NewStaffPass123!");
        request.setRole("ROLE_STAFF");

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", is("ACCESS_DENIED")));
    }

    @Test
    @WithMockUser(username = "admin_user", roles = {"ADMIN"})
    @DisplayName("ADMIN user cannot create new user accounts (OWNER only)")
    void testAdminCannotCreateUser() throws Exception {
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("admin_created");
        request.setEmail("admincreated@example.com");
        request.setFullName("Admin Created");
        request.setPassword("Pass12345678!");
        request.setRole("ROLE_STAFF");

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", is("ACCESS_DENIED")));
    }

    @Test
    @WithMockUser(username = "owner_user", roles = {"OWNER"})
    @DisplayName("User list response does not expose password hashes")
    void testUserListDoesNotExposePasswords() throws Exception {
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].passwordHash").doesNotExist())
                .andExpect(jsonPath("$.data[*].password").doesNotExist());
    }
}
