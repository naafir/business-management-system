package com.antigravity.billing.controller;

import com.antigravity.billing.dto.common.ApiResponse;
import com.antigravity.billing.dto.common.PageResponse;
import com.antigravity.billing.dto.document.DocumentResponseDto;
import com.antigravity.billing.entity.Document;
import com.antigravity.billing.security.CustomUserDetails;
import com.antigravity.billing.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<DocumentResponseDto>> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "entityType", required = false, defaultValue = "GENERAL") String entityType,
            @RequestParam(value = "entityId", required = false) UUID entityId,
            @RequestParam(value = "notes", required = false) String notes,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        DocumentResponseDto dto = documentService.uploadDocument(
                file,
                entityType,
                entityId,
                notes,
                userDetails.getId(),
                userDetails.getUsername()
        );
        return new ResponseEntity<>(ApiResponse.ok("File uploaded successfully", dto), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<DocumentResponseDto>>> listDocuments(
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        PageResponse<DocumentResponseDto> response = documentService.listDocuments(entityType, search, pageable);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadDocument(@PathVariable UUID id) {
        Document doc = documentService.getDocument(id);
        Resource resource = documentService.loadDocumentFile(id);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(doc.getFileType() != null ? doc.getFileType() : "application/octet-stream"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + doc.getFileName() + "\"")
                .body(resource);
    }

    @GetMapping("/{id}/preview")
    public ResponseEntity<Resource> previewDocument(@PathVariable UUID id) {
        Document doc = documentService.getDocument(id);
        Resource resource = documentService.loadDocumentFile(id);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(doc.getFileType() != null ? doc.getFileType() : "application/octet-stream"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + doc.getFileName() + "\"")
                .body(resource);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteDocument(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        documentService.deleteDocument(id, userDetails.getId(), userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok("Document deleted successfully", null));
    }
}
