package com.antigravity.billing.service.impl;

import com.antigravity.billing.dto.common.PageResponse;
import com.antigravity.billing.dto.product.*;
import com.antigravity.billing.entity.GstRate;
import com.antigravity.billing.entity.InventoryTransaction;
import com.antigravity.billing.entity.InventoryTransactionType;
import com.antigravity.billing.entity.Product;
import com.antigravity.billing.entity.ProductCategory;
import com.antigravity.billing.exception.ApiException;
import com.antigravity.billing.exception.ResourceNotFoundException;
import com.antigravity.billing.repository.GstRateRepository;
import com.antigravity.billing.repository.InventoryTransactionRepository;
import com.antigravity.billing.repository.ProductCategoryRepository;
import com.antigravity.billing.repository.ProductRepository;
import com.antigravity.billing.service.AuditService;
import com.antigravity.billing.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductCategoryRepository categoryRepository;
    private final GstRateRepository gstRateRepository;
    private final InventoryTransactionRepository transactionRepository;
    private final AuditService auditService;

    @Override
    @Transactional
    public ProductResponseDto createProduct(CreateProductRequest request, UUID userId, String username) {
        if (productRepository.existsBySkuIgnoreCase(request.getSku().trim())) {
            throw new ApiException("Product with SKU '" + request.getSku() + "' already exists", HttpStatus.CONFLICT);
        }

        ProductCategory category = null;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.getCategoryId()));
        }

        GstRate gstRate = null;
        if (request.getGstRateId() != null) {
            gstRate = gstRateRepository.findById(request.getGstRateId())
                    .orElseThrow(() -> new ResourceNotFoundException("GstRate", "id", request.getGstRateId()));
        }

        Product product = Product.builder()
                .sku(request.getSku().trim().toUpperCase())
                .name(request.getName().trim())
                .description(request.getDescription())
                .category(category)
                .brand(request.getBrand())
                .hsnSac(request.getHsnSac().trim())
                .unit(request.getUnit().trim().toUpperCase())
                .purchasePrice(request.getPurchasePrice())
                .sellingPrice(request.getSellingPrice())
                .gstRate(gstRate)
                .openingStock(request.getOpeningStock())
                .currentStock(request.getOpeningStock())
                .minStockLevel(request.getMinStockLevel())
                .allowNegativeStock(request.isAllowNegativeStock())
                .active(true)
                .build();

        Product saved = productRepository.save(product);

        if (saved.getOpeningStock() != null && saved.getOpeningStock().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal unitCost = saved.getPurchasePrice() != null ? saved.getPurchasePrice() : BigDecimal.ZERO;
            InventoryTransaction openingTx = InventoryTransaction.builder()
                    .product(saved)
                    .transactionType(InventoryTransactionType.OPENING_STOCK)
                    .quantity(saved.getOpeningStock())
                    .previousStock(BigDecimal.ZERO)
                    .newStock(saved.getOpeningStock())
                    .unitCost(unitCost)
                    .totalCost(unitCost.multiply(saved.getOpeningStock()))
                    .referenceType("INITIAL_OPENING")
                    .referenceId(saved.getId())
                    .referenceNumber("OPENING-" + saved.getSku())
                    .notes("Initial opening stock balance recorded upon product creation")
                    .createdBy(userId)
                    .createdByName(username)
                    .createdAt(Instant.now())
                    .build();
            transactionRepository.save(openingTx);
        }

        auditService.logAction(
                userId,
                username,
                "CREATE_PRODUCT",
                "Product",
                saved.getId().toString(),
                "Created product: " + saved.getName() + " (" + saved.getSku() + ")"
        );

        return mapToDto(saved);
    }

    @Override
    @Transactional
    public ProductResponseDto updateProduct(UUID id, UpdateProductRequest request, UUID userId, String username) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));

        ProductCategory category = null;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.getCategoryId()));
        }

        GstRate gstRate = null;
        if (request.getGstRateId() != null) {
            gstRate = gstRateRepository.findById(request.getGstRateId())
                    .orElseThrow(() -> new ResourceNotFoundException("GstRate", "id", request.getGstRateId()));
        }

        product.setName(request.getName().trim());
        product.setDescription(request.getDescription());
        product.setCategory(category);
        product.setBrand(request.getBrand());
        product.setHsnSac(request.getHsnSac().trim());
        product.setUnit(request.getUnit().trim().toUpperCase());
        product.setPurchasePrice(request.getPurchasePrice());
        product.setSellingPrice(request.getSellingPrice());
        product.setGstRate(gstRate);
        product.setMinStockLevel(request.getMinStockLevel());
        product.setAllowNegativeStock(request.isAllowNegativeStock());

        Product updated = productRepository.save(product);

        auditService.logAction(
                userId,
                username,
                "UPDATE_PRODUCT",
                "Product",
                updated.getId().toString(),
                "Updated product: " + updated.getName()
        );

        return mapToDto(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponseDto getProductById(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
        return mapToDto(product);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductResponseDto> listProducts(String search, UUID categoryId, Boolean activeOnly, Pageable pageable) {
        String searchTerm = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        Page<Product> page = productRepository.findProductsWithFilters(searchTerm, categoryId, activeOnly, pageable);
        return PageResponse.of(page.map(this::mapToDto));
    }

    @Override
    @Transactional
    public ProductResponseDto toggleProductStatus(UUID id, boolean active, UUID userId, String username) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));

        product.setActive(active);
        Product updated = productRepository.save(product);

        auditService.logAction(
                userId,
                username,
                active ? "ACTIVATE_PRODUCT" : "DEACTIVATE_PRODUCT",
                "Product",
                updated.getId().toString(),
                (active ? "Activated" : "Deactivated") + " product: " + updated.getName()
        );

        return mapToDto(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductCategoryDto> listCategories() {
        return categoryRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(cat -> ProductCategoryDto.builder()
                        .id(cat.getId())
                        .name(cat.getName())
                        .description(cat.getDescription())
                        .active(cat.isActive())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ProductCategoryDto createCategory(CreateProductCategoryRequest request, UUID userId, String username) {
        if (categoryRepository.existsByNameIgnoreCase(request.getName().trim())) {
            throw new ApiException("Category '" + request.getName() + "' already exists", HttpStatus.CONFLICT);
        }

        ProductCategory cat = ProductCategory.builder()
                .name(request.getName().trim())
                .description(request.getDescription())
                .active(true)
                .build();

        ProductCategory saved = categoryRepository.save(cat);

        auditService.logAction(
                userId,
                username,
                "CREATE_CATEGORY",
                "ProductCategory",
                saved.getId().toString(),
                "Created category: " + saved.getName()
        );

        return ProductCategoryDto.builder()
                .id(saved.getId())
                .name(saved.getName())
                .description(saved.getDescription())
                .active(saved.isActive())
                .build();
    }

    private ProductResponseDto mapToDto(Product p) {
        boolean isLow = p.getCurrentStock().compareTo(p.getMinStockLevel()) <= 0;
        return ProductResponseDto.builder()
                .id(p.getId())
                .sku(p.getSku())
                .name(p.getName())
                .description(p.getDescription())
                .categoryId(p.getCategory() != null ? p.getCategory().getId() : null)
                .categoryName(p.getCategory() != null ? p.getCategory().getName() : "Uncategorized")
                .brand(p.getBrand())
                .hsnSac(p.getHsnSac())
                .unit(p.getUnit())
                .purchasePrice(p.getPurchasePrice())
                .sellingPrice(p.getSellingPrice())
                .gstRateId(p.getGstRate() != null ? p.getGstRate().getId() : null)
                .gstRatePercent(p.getGstRate() != null ? p.getGstRate().getRatePercent() : BigDecimal.ZERO)
                .openingStock(p.getOpeningStock())
                .currentStock(p.getCurrentStock())
                .minStockLevel(p.getMinStockLevel())
                .isLowStock(isLow)
                .allowNegativeStock(p.isAllowNegativeStock())
                .active(p.isActive())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}