package com.antigravity.billing.controller;

import com.antigravity.billing.dto.audit.AuditLogResponseDto;
import com.antigravity.billing.dto.common.ApiResponse;
import com.antigravity.billing.dto.common.PageResponse;
import com.antigravity.billing.entity.AuditLog;
import com.antigravity.billing.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditLogRepository auditLogRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AuditLogResponseDto>>> getAuditLogs(
            @RequestParam(required = false) String entityType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<AuditLog> auditPage;
        if (entityType != null && !entityType.isBlank()) {
            auditPage = auditLogRepository.findByEntityTypeOrderByCreatedAtDesc(entityType, pageable);
        } else {
            auditPage = auditLogRepository.findAllByOrderByCreatedAtDesc(pageable);
        }
        Page<AuditLogResponseDto> dtoPage = auditPage.map(log -> AuditLogResponseDto.builder()
                .id(log.getId())
                .userId(log.getUserId())
                .username(log.getUsername())
                .action(log.getAction())
                .entityType(log.getEntityType())
                .entityId(log.getEntityId())
                .details(log.getDetails())
                .createdAt(log.getCreatedAt())
                .build());
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.of(dtoPage)));
    }
}
