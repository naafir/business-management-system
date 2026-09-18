package com.antigravity.billing.controller;

import com.antigravity.billing.dto.backup.BackupItemDto;
import com.antigravity.billing.dto.common.ApiResponse;
import com.antigravity.billing.security.CustomUserDetails;
import com.antigravity.billing.service.BackupService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/backups")
@RequiredArgsConstructor
public class BackupController {

    private final BackupService backupService;

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<BackupItemDto>>> listBackups() {
        List<BackupItemDto> backups = backupService.listBackups();
        return ResponseEntity.ok(ApiResponse.ok(backups));
    }

    @PostMapping("/trigger")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<BackupItemDto>> triggerBackup(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        BackupItemDto backup = backupService.triggerBackup(userDetails.getId(), userDetails.getUsername());
        return new ResponseEntity<>(ApiResponse.ok("Database backup created successfully", backup), HttpStatus.CREATED);
    }

    @GetMapping("/{fileName}/download")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<Resource> downloadBackup(@PathVariable String fileName) {
        Resource resource = backupService.loadBackupResource(fileName);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .body(resource);
    }
}
