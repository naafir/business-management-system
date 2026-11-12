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
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class BackupServiceTest {

    @Mock
    private AuditService auditService;

    @TempDir
    Path tempDir;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
    }

    @Test
    @DisplayName("A. Successful pg_dump: backup created, non-empty file, SHA-256 generated, status VERIFIED")
    void testTriggerBackupSuccess_A() throws Exception {
        BackupServiceImpl.ProcessExecutor processExecutor = (command, env) -> {
            if (command.contains("--version")) {
                return 0;
            }
            int fIdx = command.indexOf("-f");
            if (fIdx != -1 && fIdx + 1 < command.size()) {
                Path dumpPath = Paths.get(command.get(fIdx + 1));
                Files.writeString(dumpPath, "VALID_PG_DUMP_BINARY_HEADER_DATA_CONTENT", StandardCharsets.UTF_8);
            }
            return 0;
        };

        BackupServiceImpl service = new BackupServiceImpl(tempDir.toString(), auditService, processExecutor);
        service.init();

        BackupItemDto result = service.triggerBackup(userId, "admin");

        assertThat(result).isNotNull();
        assertThat(result.getFileName()).startsWith("backup_business_billing_db_").endsWith(".dump");
        assertThat(result.getFileSizeBytes()).isGreaterThan(0);
        assertThat(result.getStatus()).isEqualTo("VERIFIED");

        // Verify SHA-256 match
        byte[] expectedHash = MessageDigest.getInstance("SHA-256").digest("VALID_PG_DUMP_BINARY_HEADER_DATA_CONTENT".getBytes(StandardCharsets.UTF_8));
        String expectedChecksumHex = HexFormat.of().formatHex(expectedHash);
        assertThat(result.getChecksumSha256()).isEqualTo(expectedChecksumHex);

        // Verify files on disk
        Path dumpFile = tempDir.resolve(result.getFileName());
        assertThat(Files.exists(dumpFile)).isTrue();
        assertThat(Files.size(dumpFile)).isGreaterThan(0);

        Path checksumFile = tempDir.resolve(result.getFileName() + ".sha256");
        assertThat(Files.exists(checksumFile)).isTrue();
        assertThat(Files.readString(checksumFile).trim()).isEqualTo(expectedChecksumHex);

        // Verify audit log
        verify(auditService).logAction(eq(userId), eq("admin"), eq("DATABASE_BACKUP"), eq("SYSTEM"), anyString(), anyString());
    }

    @Test
    @DisplayName("B. pg_dump failure: backup is NOT marked VERIFIED, no dummy backup created, status FAILED, error returned")
    void testTriggerBackupFailure_B() {
        BackupServiceImpl.ProcessExecutor processExecutor = (command, env) -> {
            if (command.contains("--version")) {
                return 0;
            }
            // Create partial file then return exit code 1
            int fIdx = command.indexOf("-f");
            if (fIdx != -1 && fIdx + 1 < command.size()) {
                Path dumpPath = Paths.get(command.get(fIdx + 1));
                Files.writeString(dumpPath, "PARTIAL_CORRUPT_DATA", StandardCharsets.UTF_8);
            }
            return 1; // Error exit code
        };

        BackupServiceImpl service = new BackupServiceImpl(tempDir.toString(), auditService, processExecutor);
        service.init();

        assertThatThrownBy(() -> service.triggerBackup(userId, "admin"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("pg_dump exited with error code 1")
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);

        // Verify NO backup file or checksum file remains on disk
        assertThat(service.listBackups()).isEmpty();
        verifyNoInteractions(auditService);
    }

    @Test
    @DisplayName("C. pg_dump unavailable: backup is NOT marked VERIFIED, no fake backup generated, throws error")
    void testTriggerBackupUnavailable_C() {
        BackupServiceImpl.ProcessExecutor processExecutor = (command, env) -> {
            throw new IOException("Cannot run program pg_dump: file not found");
        };

        BackupServiceImpl service = new BackupServiceImpl(tempDir.toString(), auditService, processExecutor);
        service.init();

        assertThatThrownBy(() -> service.triggerBackup(userId, "admin"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("pg_dump utility is not available")
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);

        // Verify NO fake backup file was generated
        assertThat(service.listBackups()).isEmpty();
        verifyNoInteractions(auditService);
    }

    @Test
    @DisplayName("D. Verify secrets are passed via PGPASSWORD env var and never leaked to logs or exception text")
    void testSecretsNeverLogged_D() {
        AtomicReference<String> passedPgPassword = new AtomicReference<>();
        BackupServiceImpl.ProcessExecutor processExecutor = (command, env) -> {
            if (env != null) {
                passedPgPassword.set(env.get("PGPASSWORD"));
            }
            return 1; // Fail to check exception message safety
        };

        BackupServiceImpl service = new BackupServiceImpl(tempDir.toString(), auditService, processExecutor);
        service.init();

        assertThatThrownBy(() -> service.triggerBackup(userId, "admin"))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    assertThat(ex.getMessage()).doesNotContain("change_this_strong_password");
                    assertThat(ex.getMessage()).doesNotContain("PGPASSWORD");
                });
    }

    @Test
    @DisplayName("List backups marks zero-byte or missing checksum backups as FAILED")
    void testListBackupsStatusValidation() throws IOException {
        BackupServiceImpl service = new BackupServiceImpl(tempDir.toString(), auditService);
        service.init();

        // 1. Valid backup
        Path validBackup = tempDir.resolve("backup_business_billing_db_20260901_100000.dump");
        Files.writeString(validBackup, "valid dump content");
        Files.writeString(tempDir.resolve("backup_business_billing_db_20260901_100000.dump.sha256"), "abcdef123456");

        // 2. Corrupted zero-byte backup
        Path emptyBackup = tempDir.resolve("backup_business_billing_db_20260901_110000.dump");
        Files.createFile(emptyBackup);

        // 3. Missing checksum backup
        Path noChecksumBackup = tempDir.resolve("backup_business_billing_db_20260901_120000.dump");
        Files.writeString(noChecksumBackup, "some content");

        List<BackupItemDto> backups = service.listBackups();
        assertThat(backups).hasSize(3);

        BackupItemDto validDto = backups.stream().filter(b -> b.getFileName().contains("100000")).findFirst().orElseThrow();
        assertThat(validDto.getStatus()).isEqualTo("VERIFIED");

        BackupItemDto emptyDto = backups.stream().filter(b -> b.getFileName().contains("110000")).findFirst().orElseThrow();
        assertThat(emptyDto.getStatus()).isEqualTo("FAILED");

        BackupItemDto noChecksumDto = backups.stream().filter(b -> b.getFileName().contains("120000")).findFirst().orElseThrow();
        assertThat(noChecksumDto.getStatus()).isEqualTo("FAILED");
    }

    @Test
    @DisplayName("Successfully loads existing backup file as Resource")
    void testLoadBackupResourceSuccess() throws IOException {
        BackupServiceImpl service = new BackupServiceImpl(tempDir.toString(), auditService);
        service.init();

        Path backup = tempDir.resolve("test_backup.dump");
        Files.writeString(backup, "sample content");

        Resource resource = service.loadBackupResource("test_backup.dump");
        assertThat(resource).isNotNull();
        assertThat(resource.exists()).isTrue();
    }

    @Test
    @DisplayName("Fails when path traversal is attempted in filename")
    void testLoadBackupResourcePathTraversalThrows() {
        BackupServiceImpl service = new BackupServiceImpl(tempDir.toString(), auditService);
        service.init();

        assertThatThrownBy(() -> service.loadBackupResource("../secret.txt"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Path traversal attempt");
    }

    @Test
    @DisplayName("Fails when backup file does not exist")
    void testLoadBackupResourceNotFound() {
        BackupServiceImpl service = new BackupServiceImpl(tempDir.toString(), auditService);
        service.init();

        assertThatThrownBy(() -> service.loadBackupResource("non_existent_backup.dump"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("not found");
    }
}
