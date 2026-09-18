package com.antigravity.billing.dto.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentResponseDto {
    private UUID id;
    private String fileName;
    private String fileType;
    private Long fileSizeBytes;
    private String filePath;
    private String entityType;
    private UUID entityId;
    private String notes;
    private UUID uploadedBy;
    private String uploadedByName;
    private Instant createdAt;
}
