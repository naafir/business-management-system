package com.antigravity.billing.controller;

import com.antigravity.billing.dto.common.ApiResponse;
import com.antigravity.billing.dto.common.PageResponse;
import com.antigravity.billing.dto.product.*;
import com.antigravity.billing.security.CustomUserDetails;
import com.antigravity.billing.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ProductResponseDto>>> listProducts(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) Boolean activeOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        PageResponse<ProductResponseDto> response = productService.listProducts(search, categoryId, activeOnly, pageable);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponseDto>> getProduct(@PathVariable UUID id) {
        ProductResponseDto product = productService.getProductById(id);
        return ResponseEntity.ok(ApiResponse.ok(product));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponseDto>> createProduct(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CreateProductRequest request
    ) {
        ProductResponseDto created = productService.createProduct(
                request,
                userDetails.getId(),
                userDetails.getUsername()
        );
        return new ResponseEntity<>(ApiResponse.ok("Product created successfully", created), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponseDto>> updateProduct(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody UpdateProductRequest request
    ) {
        ProductResponseDto updated = productService.updateProduct(
                id,
                request,
                userDetails.getId(),
                userDetails.getUsername()
        );
        return ResponseEntity.ok(ApiResponse.ok("Product updated successfully", updated));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<ProductResponseDto>> toggleStatus(
            @PathVariable UUID id,
            @RequestParam boolean active,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        ProductResponseDto updated = productService.toggleProductStatus(
                id,
                active,
                userDetails.getId(),
                userDetails.getUsername()
        );
        return ResponseEntity.ok(ApiResponse.ok("Product status updated successfully", updated));
    }

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<ProductCategoryDto>>> listCategories() {
        List<ProductCategoryDto> categories = productService.listCategories();
        return ResponseEntity.ok(ApiResponse.ok(categories));
    }

    @PostMapping("/categories")
    public ResponseEntity<ApiResponse<ProductCategoryDto>> createCategory(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CreateProductCategoryRequest request
    ) {
        ProductCategoryDto created = productService.createCategory(
                request,
                userDetails.getId(),
                userDetails.getUsername()
        );
        return new ResponseEntity<>(ApiResponse.ok("Category created successfully", created), HttpStatus.CREATED);
    }
}