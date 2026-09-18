package com.antigravity.billing.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDetailDto {
    private UUID id;
    private String username;
    private String email;
    private String fullName;
    private String role;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;
}
