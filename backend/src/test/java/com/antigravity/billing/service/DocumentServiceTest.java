package com.antigravity.billing.service;

import com.antigravity.billing.dto.common.PageResponse;
import com.antigravity.billing.dto.document.DocumentResponseDto;
import com.antigravity.billing.entity.Document;
import com.antigravity.billing.exception.ApiException;
import com.antigravity.billing.exception.ResourceNotFoundException;
import com.antigravity.billing.repository.DocumentRepository;
import com.antigravity.billing.service.impl.DocumentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private StorageService storageService;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private DocumentServiceImpl documentService;

    private Document testDoc;
    private UUID docId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        docId = UUID.randomUUID();
        userId = UUID.randomUUID();
        testDoc = Document.builder()
                .fileName("tax_invoice_101.pdf")
                .fileType("application/pdf")
                .fileSizeBytes(1024L)
                .filePath("sale/stored-uuid-101.pdf")
                .entityType("SALE")
                .entityId(UUID.randomUUID())
                .notes("Original tax invoice")
                .uploadedBy(userId)
                .uploadedByName("admin")
                .build();
        testDoc.setId(docId);
        testDoc.setCreatedAt(Instant.now());
    }

    @Test
    @DisplayName("Successfully uploads document and persists metadata")
    void testUploadDocumentSuccess() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "tax_invoice_101.pdf",
                "application/pdf",
                "PDF Content Stream".getBytes()
        );

        when(storageService.storeFile(eq(file), eq("sale"))).thenReturn("sale/stored-uuid-101.pdf");
        when(documentRepository.save(any(Document.class))).thenAnswer(inv -> {
            Document d = inv.getArgument(0);
            d.setId(docId);
            d.setCreatedAt(Instant.now());
            return d;
        });

        DocumentResponseDto result = documentService.uploadDocument(
                file,
                "SALE",
                testDoc.getEntityId(),
                "Original tax invoice",
                userId,
                "admin"
        );

        assertThat(result).isNotNull();
        assertThat(result.getFileName()).isEqualTo("tax_invoice_101.pdf");
        assertThat(result.getFileType()).isEqualTo("application/pdf");
        assertThat(result.getEntityType()).isEqualTo("SALE");
        assertThat(result.getFilePath()).isEqualTo("sale/stored-uuid-101.pdf");
        verify(storageService).storeFile(eq(file), eq("sale"));
        verify(documentRepository).save(any(Document.class));
        verify(auditService).logAction(eq(userId), eq("admin"), eq("UPLOAD_DOCUMENT"), eq("DOCUMENT"), anyString(), anyString());
    }

    @Test
    @DisplayName("Fails to upload when file is empty")
    void testUploadEmptyFileThrows() {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "empty.pdf",
                "application/pdf",
                new byte[0]
        );

        assertThatThrownBy(() -> documentService.uploadDocument(
                emptyFile, "SALE", UUID.randomUUID(), "Note", userId, "admin"
        )).isInstanceOf(ApiException.class).hasMessageContaining("Cannot upload an empty file");
    }

    @Test
    @DisplayName("Successfully retrieves document entity by id")
    void testGetDocumentByIdSuccess() {
        when(documentRepository.findById(docId)).thenReturn(Optional.of(testDoc));

        Document result = documentService.getDocument(docId);
        assertThat(result.getId()).isEqualTo(docId);
        assertThat(result.getFileName()).isEqualTo("tax_invoice_101.pdf");
    }

    @Test
    @DisplayName("Throws ResourceNotFoundException when document does not exist")
    void testGetDocumentByIdNotFound() {
        UUID nonExistentId = UUID.randomUUID();
        when(documentRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentService.getDocument(nonExistentId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Successfully loads document resource via storage service")
    void testLoadDocumentResource() {
        when(documentRepository.findById(docId)).thenReturn(Optional.of(testDoc));
        Resource mockResource = new ByteArrayResource("PDF Content".getBytes());
        when(storageService.loadFileAsResource("sale/stored-uuid-101.pdf")).thenReturn(mockResource);

        Resource resource = documentService.loadDocumentFile(docId);
        assertThat(resource).isNotNull();
        verify(storageService).loadFileAsResource("sale/stored-uuid-101.pdf");
    }

    @Test
    @DisplayName("Successfully deletes document and removes file from storage")
    void testDeleteDocumentSuccess() {
        when(documentRepository.findById(docId)).thenReturn(Optional.of(testDoc));

        documentService.deleteDocument(docId, userId, "admin");

        verify(storageService).deleteFile("sale/stored-uuid-101.pdf");
        verify(documentRepository).delete(testDoc);
        verify(auditService).logAction(eq(userId), eq("admin"), eq("DELETE_DOCUMENT"), eq("DOCUMENT"), eq(docId.toString()), anyString());
    }

    @Test
    @DisplayName("Successfully lists documents with pagination")
    void testListDocuments() {
        Pageable pageable = PageRequest.of(0, 10);
        when(documentRepository.findDocumentsWithFilter(eq("SALE"), isNull(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(testDoc), pageable, 1));

        PageResponse<DocumentResponseDto> result = documentService.listDocuments("SALE", null, pageable);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }
}
