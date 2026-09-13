package com.antigravity.billing.dto.audit;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class AuditLogResponseDto {
    private UUID id;
    private UUID userId;
    private String username;
    private String action;
    private String entityType;
    private String entityId;
    private String details;
    private Instant createdAt;
}
