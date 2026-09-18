package com.antigravity.billing.service;

import com.antigravity.billing.dto.common.PageResponse;
import com.antigravity.billing.dto.document.DocumentResponseDto;
import com.antigravity.billing.entity.Document;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface DocumentService {

    DocumentResponseDto uploadDocument(MultipartFile file, String entityType, UUID entityId, String notes, UUID userId, String username);

    PageResponse<DocumentResponseDto> listDocuments(String entityType, String search, Pageable pageable);

    Resource loadDocumentFile(UUID id);

    Document getDocument(UUID id);

    void deleteDocument(UUID id, UUID userId, String username);
}
