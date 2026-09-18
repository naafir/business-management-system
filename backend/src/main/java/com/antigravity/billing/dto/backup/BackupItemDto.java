package com.antigravity.billing.dto.backup;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BackupItemDto {
    private String fileName;
    private Long fileSizeBytes;
    private String formattedSize;
    private Instant createdAt;
    private String checksumSha256;
    private String status;
}
