package com.antigravity.billing.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "documents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Document extends BaseEntity {

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "file_type", nullable = false, length = 100)
    private String fileType;

    @Column(name = "file_size_bytes", nullable = false)
    private Long fileSizeBytes;

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType;

    @Column(name = "entity_id")
    private UUID entityId;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "uploaded_by")
    private UUID uploadedBy;

    @Column(name = "uploaded_by_name", length = 100)
    private String uploadedByName;
}
