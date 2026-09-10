package com.antigravity.billing.service;

import java.util.UUID;

public interface AuditService {
    void logAction(UUID userId, String username, String action, String entityType, String entityId, String details);
}
