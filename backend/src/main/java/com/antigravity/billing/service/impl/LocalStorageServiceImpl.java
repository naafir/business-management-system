package com.antigravity.billing.service.impl;

import com.antigravity.billing.exception.ApiException;
import com.antigravity.billing.service.StorageService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
public class LocalStorageServiceImpl implements StorageService {

    private final Path rootLocation;

    public LocalStorageServiceImpl(@Value("${app.storage.local.dir:uploads}") String uploadDir) {
        this.rootLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(rootLocation);
            log.info("Storage directory initialized at: {}", rootLocation);
        } catch (IOException e) {
            throw new ApiException("Could not initialize storage location", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public String storeFile(MultipartFile file, String subDirectory) {
        if (file.isEmpty()) {
            throw new ApiException("Cannot store empty file", HttpStatus.BAD_REQUEST);
        }

        String originalFilename = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        if (originalFilename.contains("..")) {
            throw new ApiException("Filename contains invalid path sequence: " + originalFilename, HttpStatus.BAD_REQUEST);
        }

        String extension = "";
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex > 0) {
            extension = originalFilename.substring(dotIndex).toLowerCase();
        }

        String safeStoredName = UUID.randomUUID() + extension;

        try {
            Path targetDir = rootLocation;
            if (subDirectory != null && !subDirectory.isBlank()) {
                String cleanSub = StringUtils.cleanPath(subDirectory).replace("..", "");
                targetDir = rootLocation.resolve(cleanSub).normalize();
                Files.createDirectories(targetDir);
            }

            Path targetPath = targetDir.resolve(safeStoredName).normalize();
            if (!targetPath.startsWith(rootLocation)) {
                throw new ApiException("Cannot store file outside current directory", HttpStatus.BAD_REQUEST);
            }

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }

            // Return relative path from rootLocation
            return rootLocation.relativize(targetPath).toString().replace('\\', '/');
        } catch (IOException e) {
            log.error("Failed to store file: {}", originalFilename, e);
            throw new ApiException("Failed to store file: " + originalFilename, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public Resource loadFileAsResource(String filePath) {
        try {
            Path file = rootLocation.resolve(filePath).normalize();
            if (!file.startsWith(rootLocation)) {
                throw new ApiException("File path traversal detected", HttpStatus.BAD_REQUEST);
            }

            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new ApiException("File not found or not readable: " + filePath, HttpStatus.NOT_FOUND);
            }
        } catch (MalformedURLException e) {
            throw new ApiException("File not found: " + filePath, HttpStatus.NOT_FOUND);
        }
    }

    @Override
    public boolean deleteFile(String filePath) {
        try {
            Path file = rootLocation.resolve(filePath).normalize();
            if (!file.startsWith(rootLocation)) {
                return false;
            }
            return Files.deleteIfExists(file);
        } catch (IOException e) {
            log.warn("Could not delete file: {}", filePath, e);
            return false;
        }
    }
}
