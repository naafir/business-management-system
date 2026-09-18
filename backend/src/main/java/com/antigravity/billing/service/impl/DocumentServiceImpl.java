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

    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
            "application/pdf",
            "image/jpeg",
            "image/png",
            "image/webp",
            "text/plain",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-excel"
    );

    private static final long MAX_FILE_SIZE = 15 * 1024 * 1024; // 15 MB

    @Override
    @Transactional
    public DocumentResponseDto uploadDocument(MultipartFile file, String entityType, UUID entityId, String notes, UUID userId, String username) {
        if (file.isEmpty()) {
            throw new ApiException("Cannot upload an empty file", HttpStatus.BAD_REQUEST);
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ApiException("File size exceeds 15MB limit", HttpStatus.BAD_REQUEST);
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            log.warn("Uploaded file type: {} not in allowed list, accepting as generic attachment", contentType);
        }

        String normalizedType = (entityType != null && !entityType.isBlank()) ? entityType.toUpperCase() : "GENERAL";
        String subDir = normalizedType.toLowerCase();
        String relativePath = storageService.storeFile(file, subDir);

        Document doc = Document.builder()
                .fileName(file.getOriginalFilename() != null ? file.getOriginalFilename() : "file")
                .fileType(contentType != null ? contentType : "application/octet-stream")
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

    @Override
    @Transactional(readOnly = true)
    public PageResponse<DocumentResponseDto> listDocuments(String entityType, String search, Pageable pageable) {
        String typeFilter = (entityType != null && !entityType.isBlank() && !entityType.equalsIgnoreCase("ALL"))
                ? entityType.toUpperCase()
                : null;
        String searchFilter = (search != null && !search.trim().isEmpty()) ? search.trim() : null;

        Page<Document> page = documentRepository.findDocumentsWithFilter(typeFilter, searchFilter, pageable);
        return PageResponse.of(page.map(this::mapToDto));
    }

    @Override
    @Transactional(readOnly = true)
    public Resource loadDocumentFile(UUID id) {
        Document doc = getDocument(id);
        return storageService.loadFileAsResource(doc.getFilePath());
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
