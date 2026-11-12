package com.antigravity.billing.service.impl;

import com.antigravity.billing.dto.common.PageResponse;
import com.antigravity.billing.dto.document.DocumentResponseDto;
import com.antigravity.billing.entity.Document;
import com.antigravity.billing.exception.ApiException;
import com.antigravity.billing.exception.ResourceNotFoundException;
import com.antigravity.billing.repository.DocumentRepository;
import com.antigravity.billing.service.AuditService;
import com.antigravity.billing.service.DocumentService;
import com.antigravity.billing.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private final DocumentRepository documentRepository;
    private final StorageService storageService;
    private final AuditService auditService;

    private static final java.util.Set<String> ALLOWED_EXTENSIONS = java.util.Set.of(".pdf", ".png", ".jpg", ".jpeg");
    private static final java.util.Set<String> DANGEROUS_EXTENSIONS = java.util.Set.of(
            ".exe", ".bat", ".cmd", ".ps1", ".sh", ".php", ".jsp", ".js",
            ".html", ".htm", ".svg", ".jar", ".class", ".vbs", ".dll"
    );
    private static final java.util.Set<String> ALLOWED_MIME_TYPES = java.util.Set.of(
            "application/pdf",
            "image/png",
            "image/jpeg"
    );

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB

    @Override
    @Transactional
    public DocumentResponseDto uploadDocument(MultipartFile file, String entityType, UUID entityId, String notes, UUID userId, String username) {
        validateFile(file);

        String contentType = file.getContentType() != null ? file.getContentType().toLowerCase() : "application/octet-stream";
        String normalizedType = (entityType != null && !entityType.isBlank()) ? entityType.toUpperCase() : "GENERAL";
        String subDir = normalizedType.toLowerCase();
        String relativePath = storageService.storeFile(file, subDir);

        String originalName = org.springframework.util.StringUtils.cleanPath(
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "file"
        ).replaceAll("[^a-zA-Z0-9._-]", "_");

        Document doc = Document.builder()
                .fileName(originalName)
                .fileType(contentType)
                .fileSizeBytes(file.getSize())
                .filePath(relativePath)
                .entityType(normalizedType)
                .entityId(entityId)
                .notes(notes)
                .uploadedBy(userId)
                .uploadedByName(username)
                .build();

        Document saved = documentRepository.save(doc);

        auditService.logAction(
                userId,
                username,
                "UPLOAD_DOCUMENT",
                "DOCUMENT",
                saved.getId().toString(),
                "Uploaded attachment: " + saved.getFileName() + " (" + (saved.getFileSizeBytes() / 1024) + " KB) for " + normalizedType
        );

        return mapToDto(saved);
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException("Cannot upload an empty file", HttpStatus.BAD_REQUEST);
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ApiException("File size exceeds 10MB limit", HttpStatus.BAD_REQUEST);
        }

        String rawFilename = file.getOriginalFilename();
        if (rawFilename == null || rawFilename.isBlank()) {
            throw new ApiException("Invalid filename", HttpStatus.BAD_REQUEST);
        }

        String cleanFilename = org.springframework.util.StringUtils.cleanPath(rawFilename);
        if (cleanFilename.contains("..") || cleanFilename.contains("/") || cleanFilename.contains("\\")) {
            throw new ApiException("Filename contains invalid path sequence", HttpStatus.BAD_REQUEST);
        }

        String lowerName = cleanFilename.toLowerCase();
        for (String dangerousExt : DANGEROUS_EXTENSIONS) {
            if (lowerName.contains(dangerousExt)) {
                throw new ApiException("Dangerous or executable file extension prohibited", HttpStatus.BAD_REQUEST);
            }
        }

        int lastDotIndex = lowerName.lastIndexOf('.');
        if (lastDotIndex <= 0) {
            throw new ApiException("File must have a valid extension (.pdf, .png, .jpg, .jpeg)", HttpStatus.BAD_REQUEST);
        }

        String extension = lowerName.substring(lastDotIndex);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new ApiException("Unsupported file type extension: " + extension, HttpStatus.BAD_REQUEST);
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
            throw new ApiException("Unsupported MIME content type: " + contentType, HttpStatus.BAD_REQUEST);
        }

        // Validate actual file content header magic bytes
        try (java.io.InputStream is = file.getInputStream()) {
            byte[] header = is.readNBytes(8);
            if (header.length < 4) {
                throw new ApiException("Invalid file content signature", HttpStatus.BAD_REQUEST);
            }

            if (extension.equals(".pdf")) {
                if (header[0] != 0x25 || header[1] != 0x50 || header[2] != 0x44 || header[3] != 0x46) {
                    throw new ApiException("File content magic bytes do not match PDF format", HttpStatus.BAD_REQUEST);
                }
            } else if (extension.equals(".png")) {
                if (header[0] != (byte) 0x89 || header[1] != 0x50 || header[2] != 0x4E || header[3] != 0x47) {
                    throw new ApiException("File content magic bytes do not match PNG format", HttpStatus.BAD_REQUEST);
                }
            } else if (extension.equals(".jpg") || extension.equals(".jpeg")) {
                if (header[0] != (byte) 0xFF || header[1] != (byte) 0xD8 || header[2] != (byte) 0xFF) {
                    throw new ApiException("File content magic bytes do not match JPEG format", HttpStatus.BAD_REQUEST);
                }
            }
        } catch (ApiException ae) {
            throw ae;
        } catch (Exception e) {
            throw new ApiException("Could not read file content for validation", HttpStatus.BAD_REQUEST);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<DocumentResponseDto> listDocuments(String entityType, String search, Pageable pageable) {
        String typeFilter = (entityType != null && !entityType.isBlank() && !entityType.equalsIgnoreCase("ALL"))
                ? entityType.toUpperCase()
                : null;
        String searchFilter = (search != null && !search.trim().isEmpty())
                ? "%" + search.trim().toLowerCase() + "%"
                : null;

        Page<Document> page = documentRepository.findDocumentsWithFilter(typeFilter, searchFilter, pageable);
        return PageResponse.of(page.map(this::mapToDto));
    }

    @Override
    @Transactional(readOnly = true)
    public Resource loadDocumentFile(UUID id) {
        Document doc = getDocument(id);
        try {
            return storageService.loadFileAsResource(doc.getFilePath());
        } catch (ApiException ae) {
            throw ae;
        } catch (Exception e) {
            throw new ApiException("Document file not found on disk", HttpStatus.NOT_FOUND);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Document getDocument(UUID id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document", "id", id));
    }

    @Override
    @Transactional
    public void deleteDocument(UUID id, UUID userId, String username) {
        Document doc = getDocument(id);
        storageService.deleteFile(doc.getFilePath());
        documentRepository.delete(doc);

        auditService.logAction(
                userId,
                username,
                "DELETE_DOCUMENT",
                "DOCUMENT",
                id.toString(),
                "Deleted attachment: " + doc.getFileName()
        );
    }

    private DocumentResponseDto mapToDto(Document d) {
        return DocumentResponseDto.builder()
                .id(d.getId())
                .fileName(d.getFileName())
                .fileType(d.getFileType())
                .fileSizeBytes(d.getFileSizeBytes())
                .filePath(d.getFilePath())
                .entityType(d.getEntityType())
                .entityId(d.getEntityId())
                .notes(d.getNotes())
                .uploadedBy(d.getUploadedBy())
                .uploadedByName(d.getUploadedByName())
                .createdAt(d.getCreatedAt())
                .build();
    }
}
