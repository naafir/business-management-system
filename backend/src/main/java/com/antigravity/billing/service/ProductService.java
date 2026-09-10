package com.antigravity.billing.service;

import com.antigravity.billing.dto.common.PageResponse;
import com.antigravity.billing.dto.product.*;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface ProductService {
    ProductResponseDto createProduct(CreateProductRequest request, UUID userId, String username);
    ProductResponseDto updateProduct(UUID id, UpdateProductRequest request, UUID userId, String username);
    ProductResponseDto getProductById(UUID id);
    PageResponse<ProductResponseDto> listProducts(String search, UUID categoryId, Boolean activeOnly, Pageable pageable);
    ProductResponseDto toggleProductStatus(UUID id, boolean active, UUID userId, String username);
    List<ProductCategoryDto> listCategories();
    ProductCategoryDto createCategory(CreateProductCategoryRequest request, UUID userId, String username);
}