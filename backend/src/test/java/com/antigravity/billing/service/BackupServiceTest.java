package com.antigravity.billing.service;

import com.antigravity.billing.dto.backup.BackupItemDto;
import com.antigravity.billing.exception.ApiException;
import com.antigravity.billing.service.impl.BackupServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BackupServiceTest {

    @Mock
    private AuditService auditService;

    @TempDir
    Path tempDir;

    private BackupServiceImpl backupService;

    @BeforeEach
    void setUp() {
        backupService = new BackupServiceImpl(tempDir.toString(), auditService);
        backupService.init();
    }

    @Test
    @DisplayName("Successfully triggers backup and writes snapshot with SHA-256 checksum")
    void testTriggerBackupSuccess() {
        UUID userId = UUID.randomUUID();
        BackupItemDto result = backupService.triggerBackup(userId, "admin");

        assertThat(result).isNotNull();
        assertThat(result.getFileName()).startsWith("backup_business_billing_db_").endsWith(".dump");
        assertThat(result.getFileSizeBytes()).isGreaterThan(0);
        assertThat(result.getChecksumSha256()).isNotNull().hasSize(64);
        assertThat(result.getStatus()).isEqualTo("VERIFIED");

        // Verify audit log
        verify(auditService).logAction(eq(userId), eq("admin"), eq("DATABASE_BACKUP"), eq("SYSTEM"), anyString(), anyString());

        // Verify file exists on disk
        Path dumpFile = tempDir.resolve(result.getFileName());
        assertThat(Files.exists(dumpFile)).isTrue();

        Path checksumFile = tempDir.resolve(result.getFileName() + ".sha256");
        assertThat(Files.exists(checksumFile)).isTrue();
    }

    @Test
    @DisplayName("Successfully lists available backups sorted by modification time")
    void testListBackups() throws IOException {
        Path backup1 = tempDir.resolve("backup_business_billing_db_20260901_100000.dump");
        Files.writeString(backup1, "dump content 1");
        Files.writeString(tempDir.resolve("backup_business_billing_db_20260901_100000.dump.sha256"), "abcdef123456");

        List<BackupItemDto> backups = backupService.listBackups();
        assertThat(backups).hasSize(1);
        assertThat(backups.get(0).getFileName()).isEqualTo("backup_business_billing_db_20260901_100000.dump");
        assertThat(backups.get(0).getChecksumSha256()).isEqualTo("abcdef123456");
        assertThat(backups.get(0).getStatus()).isEqualTo("VERIFIED");
    }

    @Test
    @DisplayName("Successfully loads existing backup file as Resource")
    void testLoadBackupResourceSuccess() throws IOException {
        Path backup = tempDir.resolve("test_backup.dump");
        Files.writeString(backup, "sample content");

        Resource resource = backupService.loadBackupResource("test_backup.dump");
        assertThat(resource).isNotNull();
        assertThat(resource.exists()).isTrue();
    }

    @Test
    @DisplayName("Fails when path traversal is attempted in filename")
    void testLoadBackupResourcePathTraversalThrows() {
        assertThatThrownBy(() -> backupService.loadBackupResource("../secret.txt"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Path traversal attempt");
    }

    @Test
    @DisplayName("Fails when backup file does not exist")
    void testLoadBackupResourceNotFound() {
        assertThatThrownBy(() -> backupService.loadBackupResource("non_existent_backup.dump"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("not found");
    }
}
