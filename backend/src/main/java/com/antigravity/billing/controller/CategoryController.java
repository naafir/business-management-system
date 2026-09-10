package com.antigravity.billing.controller;

import com.antigravity.billing.dto.common.ApiResponse;
import com.antigravity.billing.dto.product.CreateProductCategoryRequest;
import com.antigravity.billing.dto.product.ProductCategoryDto;
import com.antigravity.billing.security.CustomUserDetails;
import com.antigravity.billing.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProductCategoryDto>>> listCategories() {
        List<ProductCategoryDto> categories = productService.listCategories();
        return ResponseEntity.ok(ApiResponse.ok(categories));
    }

    @PostMapping
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
