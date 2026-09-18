package com.antigravity.billing.dto.auth;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CreateUserRequest {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be 3–50 characters")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "Username may only contain letters, digits, dots, hyphens, underscores")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email address")
    @Size(max = 100)
    private String email;

    @NotBlank(message = "Full name is required")
    @Size(max = 100)
    private String fullName;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    @NotBlank(message = "Role is required")
    @Pattern(regexp = "ROLE_OWNER|ROLE_ADMIN|ROLE_MANAGER|ROLE_STAFF",
             message = "Role must be one of: ROLE_OWNER, ROLE_ADMIN, ROLE_MANAGER, ROLE_STAFF")
    private String role;
}
