package com.antigravity.billing.service;

import com.antigravity.billing.dto.backup.BackupItemDto;
import org.springframework.core.io.Resource;

import java.util.List;
import java.util.UUID;

public interface BackupService {

    List<BackupItemDto> listBackups();

    BackupItemDto triggerBackup(UUID userId, String username);

    Resource loadBackupResource(String fileName);
}
