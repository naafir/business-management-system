package com.antigravity.billing.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface StorageService {

    String storeFile(MultipartFile file, String subDirectory);

    Resource loadFileAsResource(String filePath);

    boolean deleteFile(String filePath);
}
