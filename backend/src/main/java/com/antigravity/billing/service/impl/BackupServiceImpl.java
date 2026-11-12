package com.antigravity.billing.service.impl;

import com.antigravity.billing.dto.backup.BackupItemDto;
import com.antigravity.billing.exception.ApiException;
import com.antigravity.billing.service.AuditService;
import com.antigravity.billing.service.BackupService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Stream;

@Slf4j
@Service
public class BackupServiceImpl implements BackupService {

    @FunctionalInterface
    public interface ProcessExecutor {
        int execute(List<String> command, Map<String, String> environment) throws IOException, InterruptedException;
    }

    public static class DefaultProcessExecutor implements ProcessExecutor {
        @Override
        public int execute(List<String> command, Map<String, String> environment) throws IOException, InterruptedException {
            ProcessBuilder pb = new ProcessBuilder(command);
            if (environment != null && !environment.isEmpty()) {
                pb.environment().putAll(environment);
            }
            Process process = pb.start();
            return process.waitFor();
        }
    }

    private final Path backupDirectory;
    private final AuditService auditService;
    private final ProcessExecutor processExecutor;

    @Value("${spring.datasource.url:jdbc:postgresql://localhost:5432/business_billing_db}")
    private String dbUrl = "jdbc:postgresql://localhost:5432/business_billing_db";

    @Value("${spring.datasource.username:postgres}")
    private String dbUser = "postgres";

    @Value("${spring.datasource.password:${DB_PASSWORD:}}")
    private String dbPassword = "";

    @Autowired
    public BackupServiceImpl(
            @Value("${app.backup.dir:backups}") String backupDir,
            AuditService auditService
    ) {
        this(backupDir, auditService, new DefaultProcessExecutor());
    }

    public BackupServiceImpl(
            String backupDir,
            AuditService auditService,
            ProcessExecutor processExecutor
    ) {
        this.backupDirectory = Paths.get(backupDir).toAbsolutePath().normalize();
        this.auditService = auditService;
        this.processExecutor = processExecutor != null ? processExecutor : new DefaultProcessExecutor();
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(backupDirectory);
            log.info("Backup storage directory initialized at: {}", backupDirectory);
        } catch (IOException e) {
            log.error("Could not create backup directory: {}", backupDirectory, e);
        }
    }

    @Override
    public List<BackupItemDto> listBackups() {
        List<BackupItemDto> results = new ArrayList<>();
        if (!Files.exists(backupDirectory)) {
            return results;
        }

        try (Stream<Path> stream = Files.list(backupDirectory)) {
            stream.filter(p -> {
                String name = p.getFileName().toString().toLowerCase();
                return (name.endsWith(".dump") || name.endsWith(".sql") || name.endsWith(".sql.gz"))
                        && !name.endsWith(".sha256");
            }).sorted((a, b) -> {
                try {
                    return Files.getLastModifiedTime(b).compareTo(Files.getLastModifiedTime(a));
                } catch (IOException e) {
                    return 0;
                }
            }).forEach(p -> {
                try {
                    String fileName = p.getFileName().toString();
                    long sizeBytes = Files.size(p);
                    Instant modified = Files.getLastModifiedTime(p).toInstant();

                    // Check for sha256 file
                    Path checksumPath = p.resolveSibling(fileName + ".sha256");
                    String checksum = null;
                    if (Files.exists(checksumPath)) {
                        checksum = Files.readString(checksumPath, StandardCharsets.UTF_8).trim();
                    }

                    boolean isVerified = checksum != null && !checksum.isBlank() && sizeBytes > 0;

                    results.add(BackupItemDto.builder()
                            .fileName(fileName)
                            .fileSizeBytes(sizeBytes)
                            .formattedSize(formatBytes(sizeBytes))
                            .createdAt(modified)
                            .checksumSha256(checksum)
                            .status(isVerified ? "VERIFIED" : "FAILED")
                            .build());
                } catch (IOException e) {
                    log.warn("Could not read backup file attributes for {}", p, e);
                }
            });
        } catch (IOException e) {
            log.error("Failed to list files in backup directory: {}", backupDirectory, e);
        }

        return results;
    }

    @Override
    public BackupItemDto triggerBackup(UUID userId, String username) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName = "backup_business_billing_db_" + timestamp + ".dump";
        Path targetFile = backupDirectory.resolve(fileName).normalize();

        log.info("Initiating database backup to: {}", targetFile);

        try {
            // 1. Verify pg_dump availability
            try {
                int checkExitCode = processExecutor.execute(List.of("pg_dump", "--version"), Map.of());
                if (checkExitCode != 0) {
                    log.error("pg_dump --version check failed with exit code: {}", checkExitCode);
                    throw new ApiException("Database backup failed: pg_dump utility returned exit code " + checkExitCode, HttpStatus.INTERNAL_SERVER_ERROR);
                }
            } catch (IOException e) {
                log.error("pg_dump executable not found on system PATH: {}", e.getMessage());
                throw new ApiException("Database backup failed: pg_dump utility is not available on system PATH", HttpStatus.INTERNAL_SERVER_ERROR);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Interrupted while checking pg_dump version");
                throw new ApiException("Database backup failed: operation interrupted", HttpStatus.INTERNAL_SERVER_ERROR);
            }

            // 2. Prepare pg_dump command arguments using externalized DB configuration
            String host = extractDbHost(dbUrl);
            String port = extractDbPort(dbUrl);
            String dbName = extractDbName(dbUrl);

            List<String> dumpCommand = List.of(
                    "pg_dump",
                    "-h", host,
                    "-p", port,
                    "-U", dbUser,
                    "-F", "c",
                    "-b",
                    "-f", targetFile.toString(),
                    dbName
            );

            Map<String, String> env = new HashMap<>();
            if (StringUtils.hasText(dbPassword)) {
                env.put("PGPASSWORD", dbPassword);
            }

            // 3. Execute pg_dump
            int dumpExitCode;
            try {
                dumpExitCode = processExecutor.execute(dumpCommand, env);
            } catch (IOException e) {
                log.error("Failed to execute pg_dump process: {}", e.getMessage());
                cleanUpFile(targetFile);
                throw new ApiException("Database backup failed: process execution error", HttpStatus.INTERNAL_SERVER_ERROR);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Interrupted while executing pg_dump");
                cleanUpFile(targetFile);
                throw new ApiException("Database backup failed: operation interrupted", HttpStatus.INTERNAL_SERVER_ERROR);
            }

            if (dumpExitCode != 0) {
                log.error("pg_dump failed with exit code: {}", dumpExitCode);
                cleanUpFile(targetFile);
                throw new ApiException("Database backup failed: pg_dump exited with error code " + dumpExitCode, HttpStatus.INTERNAL_SERVER_ERROR);
            }

            // 4. Verify target dump file exists and is non-empty
            if (!Files.exists(targetFile) || Files.size(targetFile) == 0) {
                log.error("pg_dump finished with exit code 0 but backup file is missing or empty at: {}", targetFile);
                cleanUpFile(targetFile);
                throw new ApiException("Database backup failed: generated dump file is missing or empty", HttpStatus.INTERNAL_SERVER_ERROR);
            }

            // 5. Generate SHA-256 checksum and save to .sha256 file
            byte[] fileBytes = Files.readAllBytes(targetFile);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(fileBytes);
            String checksumHex = HexFormat.of().formatHex(hash);

            Path checksumFile = backupDirectory.resolve(fileName + ".sha256");
            Files.writeString(checksumFile, checksumHex, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            long sizeBytes = Files.size(targetFile);

            auditService.logAction(
                    userId,
                    username,
                    "DATABASE_BACKUP",
                    "SYSTEM",
                    fileName,
                    "Created database backup: " + fileName + " (" + formatBytes(sizeBytes) + ") with SHA-256: " + checksumHex
            );

            return BackupItemDto.builder()
                    .fileName(fileName)
                    .fileSizeBytes(sizeBytes)
                    .formattedSize(formatBytes(sizeBytes))
                    .createdAt(Instant.now())
                    .checksumSha256(checksumHex)
                    .status("VERIFIED")
                    .build();

        } catch (ApiException ae) {
            throw ae;
        } catch (Exception e) {
            log.error("Unexpected error during database backup process", e);
            cleanUpFile(targetFile);
            throw new ApiException("Failed to create database backup: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public Resource loadBackupResource(String fileName) {
        String cleanName = StringUtils.cleanPath(fileName);
        if (cleanName.contains("..")) {
            throw new ApiException("Path traversal attempt in backup filename", HttpStatus.BAD_REQUEST);
        }

        Path filePath = backupDirectory.resolve(cleanName).normalize();
        if (!filePath.startsWith(backupDirectory)) {
            throw new ApiException("Invalid backup file path", HttpStatus.BAD_REQUEST);
        }

        try {
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new ApiException("Backup file not found: " + fileName, HttpStatus.NOT_FOUND);
            }
        } catch (MalformedURLException e) {
            throw new ApiException("Backup file URL invalid: " + fileName, HttpStatus.NOT_FOUND);
        }
    }

    private void cleanUpFile(Path path) {
        if (path != null && Files.exists(path)) {
            try {
                Files.delete(path);
            } catch (IOException e) {
                log.warn("Could not delete failed/partial backup file: {}", path, e);
            }
        }
    }

    private String extractDbName(String url) {
        if (StringUtils.hasText(url) && url.contains("/")) {
            String cleanUrl = url;
            if (cleanUrl.contains("?")) {
                cleanUrl = cleanUrl.substring(0, cleanUrl.indexOf("?"));
            }
            int lastSlash = cleanUrl.lastIndexOf("/");
            if (lastSlash != -1 && lastSlash < cleanUrl.length() - 1) {
                return cleanUrl.substring(lastSlash + 1);
            }
        }
        return "business_billing_db";
    }

    private String extractDbHost(String url) {
        if (StringUtils.hasText(url) && url.startsWith("jdbc:postgresql://")) {
            String afterPrefix = url.substring("jdbc:postgresql://".length());
            int slashIdx = afterPrefix.indexOf("/");
            String hostPort = slashIdx != -1 ? afterPrefix.substring(0, slashIdx) : afterPrefix;
            if (hostPort.contains(":")) {
                return hostPort.split(":")[0];
            } else if (StringUtils.hasText(hostPort)) {
                return hostPort;
            }
        }
        return "localhost";
    }

    private String extractDbPort(String url) {
        if (StringUtils.hasText(url) && url.startsWith("jdbc:postgresql://")) {
            String afterPrefix = url.substring("jdbc:postgresql://".length());
            int slashIdx = afterPrefix.indexOf("/");
            String hostPort = slashIdx != -1 ? afterPrefix.substring(0, slashIdx) : afterPrefix;
            if (hostPort.contains(":")) {
                String[] parts = hostPort.split(":");
                if (parts.length > 1) {
                    String portCandidate = parts[1].replaceAll("[^0-9]", "");
                    if (!portCandidate.isEmpty()) {
                        return portCandidate;
                    }
                }
            }
        }
        return "5432";
    }

    private String formatBytes(long bytes) {
        if (bytes == 0) return "0 B";
        long k = 1024;
        String[] sizes = {"B", "KB", "MB", "GB"};
        int i = (int) Math.floor(Math.log(bytes) / Math.log(k));
        return String.format("%.1f %s", bytes / Math.pow(k, i), sizes[i]);
    }
}
