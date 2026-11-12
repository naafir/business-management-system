package com.antigravity.billing.service;

import com.antigravity.billing.dto.document.DocumentResponseDto;
import com.antigravity.billing.entity.Document;
import com.antigravity.billing.entity.User;
import com.antigravity.billing.exception.ApiException;
import com.antigravity.billing.exception.ResourceNotFoundException;
import com.antigravity.billing.repository.DocumentRepository;
import com.antigravity.billing.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class DocumentSecurityIntegrationTest {

    @Autowired
    private DocumentService documentService;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StorageService storageService;

    private UUID userId;

    // Standard valid magic bytes
    private static final byte[] VALID_PDF_BYTES = "%PDF-1.7 Test Document Content Data".getBytes();
    private static final byte[] VALID_PNG_BYTES = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00};
    private static final byte[] VALID_JPEG_BYTES = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10, 0x4A, 0x46};

    @BeforeEach
    void setUp() {
        userId = userRepository.findAll().stream().findFirst().map(User::getId).orElse(UUID.randomUUID());
    }

    @Test
    @DisplayName("1. Valid PDF upload succeeds")
    void testValidPdfUploadSucceeds() {
        MockMultipartFile file = new MockMultipartFile("file", "invoice.pdf", "application/pdf", VALID_PDF_BYTES);
        DocumentResponseDto dto = documentService.uploadDocument(file, "INVOICE", UUID.randomUUID(), "Test PDF", userId, "admin");

        assertThat(dto).isNotNull();
        assertThat(dto.getFileName()).isEqualTo("invoice.pdf");
        assertThat(dto.getFileType()).isEqualTo("application/pdf");
        assertThat(dto.getFilePath()).isNotBlank();
    }

    @Test
    @DisplayName("2. Valid supported image uploads (PNG & JPEG) succeed")
    void testValidImageUploadsSucceed() {
        MockMultipartFile pngFile = new MockMultipartFile("file", "image.png", "image/png", VALID_PNG_BYTES);
        DocumentResponseDto pngDto = documentService.uploadDocument(pngFile, "PRODUCT", UUID.randomUUID(), "PNG image", userId, "admin");
        assertThat(pngDto.getFileType()).isEqualTo("image/png");

        MockMultipartFile jpgFile = new MockMultipartFile("file", "photo.jpg", "image/jpeg", VALID_JPEG_BYTES);
        DocumentResponseDto jpgDto = documentService.uploadDocument(jpgFile, "PRODUCT", UUID.randomUUID(), "JPEG photo", userId, "admin");
        assertThat(jpgDto.getFileType()).isEqualTo("image/jpeg");
    }

    @Test
    @DisplayName("3. Dangerous extension (malicious.exe) is rejected")
    void testExecutableFileRejected() {
        MockMultipartFile exeFile = new MockMultipartFile("file", "malicious.exe", "application/octet-stream", "MZ Executable".getBytes());

        assertThatThrownBy(() -> documentService.uploadDocument(exeFile, "GENERAL", null, "Dangerous file", userId, "admin"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Dangerous or executable file extension prohibited");
    }

    @Test
    @DisplayName("4. HTML upload (index.html) is rejected")
    void testHtmlUploadRejected() {
        MockMultipartFile htmlFile = new MockMultipartFile("file", "index.html", "text/html", "<html><script>alert(1)</script></html>".getBytes());

        assertThatThrownBy(() -> documentService.uploadDocument(htmlFile, "GENERAL", null, "XSS Payload", userId, "admin"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Dangerous or executable file extension prohibited");
    }

    @Test
    @DisplayName("5. SVG upload (logo.svg) is rejected")
    void testSvgUploadRejected() {
        MockMultipartFile svgFile = new MockMultipartFile("file", "logo.svg", "image/svg+xml", "<svg onload=alert(1)></svg>".getBytes());

        assertThatThrownBy(() -> documentService.uploadDocument(svgFile, "GENERAL", null, "SVG Payload", userId, "admin"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Dangerous or executable file extension prohibited");
    }

    @Test
    @DisplayName("6. Fake/misleading extension (malicious.exe.pdf or fake MIME with invalid header) is rejected")
    void testFakeExtensionOrMismatchedMagicBytesRejected() {
        // Double extension containing executable token
        MockMultipartFile doubleExtFile = new MockMultipartFile("file", "invoice.exe.pdf", "application/pdf", VALID_PDF_BYTES);
        assertThatThrownBy(() -> documentService.uploadDocument(doubleExtFile, "GENERAL", null, "Fake ext", userId, "admin"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Dangerous or executable file extension prohibited");

        // PDF extension but content is fake / not starting with %PDF-
        MockMultipartFile fakeMagicBytesFile = new MockMultipartFile("file", "hacked.pdf", "application/pdf", "Fake text payload".getBytes());
        assertThatThrownBy(() -> documentService.uploadDocument(fakeMagicBytesFile, "GENERAL", null, "Mismatched content", userId, "admin"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("magic bytes");
    }

    @Test
    @DisplayName("7. Path traversal filename (../../evil.pdf or ..\\..\\evil.pdf) is rejected")
    void testPathTraversalFilenameRejected() {
        MockMultipartFile traversalFile1 = new MockMultipartFile("file", "../../evil.pdf", "application/pdf", VALID_PDF_BYTES);
        assertThatThrownBy(() -> documentService.uploadDocument(traversalFile1, "GENERAL", null, "Traversal", userId, "admin"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("path sequence");

        MockMultipartFile traversalFile2 = new MockMultipartFile("file", "..\\..\\evil.pdf", "application/pdf", VALID_PDF_BYTES);
        assertThatThrownBy(() -> documentService.uploadDocument(traversalFile2, "GENERAL", null, "Traversal", userId, "admin"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("path sequence");
    }

    @Test
    @DisplayName("8. Oversized file (>10MB) is rejected")
    void testOversizedFileRejected() {
        byte[] largeBytes = new byte[10 * 1024 * 1024 + 100]; // 10MB + 100 bytes
        // Write PDF header magic bytes at beginning
        System.arraycopy("%PDF-1.7".getBytes(), 0, largeBytes, 0, 8);

        MockMultipartFile largeFile = new MockMultipartFile("file", "large.pdf", "application/pdf", largeBytes);
        assertThatThrownBy(() -> documentService.uploadDocument(largeFile, "GENERAL", null, "Large file", userId, "admin"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("exceeds 10MB limit");
    }

    @Test
    @DisplayName("9. Unauthorized or non-existent document access is rejected")
    void testNonExistentDocumentAccessRejected() {
        UUID randomId = UUID.randomUUID();
        assertThatThrownBy(() -> documentService.loadDocumentFile(randomId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("10. Unauthorized deletion attempts on non-existent records throw expected exception")
    void testUnauthorizedDeletionRejected() {
        UUID randomId = UUID.randomUUID();
        assertThatThrownBy(() -> documentService.deleteDocument(randomId, userId, "staffUser"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("11. Physical storage filename is server-generated (UUID format)")
    void testPhysicalFilenameIsServerGeneratedUUID() {
        MockMultipartFile file = new MockMultipartFile("file", "original_user_file.pdf", "application/pdf", VALID_PDF_BYTES);
        DocumentResponseDto dto = documentService.uploadDocument(file, "GENERAL", null, "Note", userId, "admin");

        // The relative stored path should contain a generated UUID filename, not "original_user_file.pdf"
        String storedRelativePath = dto.getFilePath();
        assertThat(storedRelativePath).doesNotContain("original_user_file");
        assertThat(storedRelativePath).endsWith(".pdf");
    }

    @Test
    @DisplayName("12. Missing physical file returns a controlled HTTP 404 error")
    void testMissingPhysicalFileReturnsControlledError() {
        Document missingDoc = Document.builder()
                .fileName("missing_file.pdf")
                .fileType("application/pdf")
                .fileSizeBytes(100L)
                .filePath("general/non_existent_uuid_12345.pdf")
                .entityType("GENERAL")
                .uploadedBy(userId)
                .uploadedByName("admin")
                .build();
        Document saved = documentRepository.save(missingDoc);

        assertThatThrownBy(() -> documentService.loadDocumentFile(saved.getId()))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }
}
