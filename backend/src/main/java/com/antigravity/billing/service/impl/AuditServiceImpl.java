package com.antigravity.billing.service.impl;

import com.antigravity.billing.entity.AuditLog;
import com.antigravity.billing.repository.AuditLogRepository;
import com.antigravity.billing.service.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

    private final AuditLogRepository auditLogRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAction(UUID userId, String username, String action, String entityType, String entityId, String details) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .userId(userId)
                    .username(username)
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .details(details)
                    .createdAt(Instant.now())
                    .build();

            auditLogRepository.save(auditLog);
        } catch (Exception ex) {
            log.error("Failed to write audit log for action: {} on entity: {}", action, entityType, ex);
        }
    }
}
