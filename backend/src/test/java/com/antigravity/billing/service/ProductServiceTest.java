package com.antigravity.billing.service;

import com.antigravity.billing.dto.product.CreateProductRequest;
import com.antigravity.billing.dto.product.ProductResponseDto;
import com.antigravity.billing.entity.Product;
import com.antigravity.billing.exception.ApiException;
import com.antigravity.billing.repository.GstRateRepository;
import com.antigravity.billing.repository.ProductCategoryRepository;
import com.antigravity.billing.repository.ProductRepository;
import com.antigravity.billing.service.impl.ProductServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductCategoryRepository categoryRepository;

    @Mock
    private GstRateRepository gstRateRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private com.antigravity.billing.repository.InventoryTransactionRepository transactionRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    @Test
    @DisplayName("Successfully creates product with opening stock")
    void testCreateProductSuccess() {
        CreateProductRequest request = new CreateProductRequest();
        request.setSku("LAP-DELL-01");
        request.setName("Dell XPS 15");
        request.setHsnSac("8471");
        request.setUnit("PCS");
        request.setPurchasePrice(new BigDecimal("90000.00"));
        request.setSellingPrice(new BigDecimal("115000.00"));
        request.setOpeningStock(new BigDecimal("10.00"));
        request.setMinStockLevel(new BigDecimal("2.00"));

        when(productRepository.existsBySkuIgnoreCase("LAP-DELL-01")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product p = invocation.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });

        UUID userId = UUID.randomUUID();
        ProductResponseDto result = productService.createProduct(request, userId, "admin");

        assertThat(result.getSku()).isEqualTo("LAP-DELL-01");
        assertThat(result.getCurrentStock()).isEqualByComparingTo("10.00");
        assertThat(result.getSellingPrice()).isEqualByComparingTo("115000.00");
        assertThat(result.isActive()).isTrue();

        verify(auditService, times(1)).logAction(eq(userId), eq("admin"), eq("CREATE_PRODUCT"), any(), any(), any());
    }

    @Test
    @DisplayName("Rejects product creation with duplicate SKU")
    void testDuplicateSkuRejection() {
        CreateProductRequest request = new CreateProductRequest();
        request.setSku("DUP-SKU-01");
        request.setName("Sample Item");
        request.setHsnSac("1234");

        when(productRepository.existsBySkuIgnoreCase("DUP-SKU-01")).thenReturn(true);

        assertThatThrownBy(() -> productService.createProduct(request, UUID.randomUUID(), "admin"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("Successfully updates existing product")
    void testUpdateProduct() {
        UUID id = UUID.randomUUID();
        Product product = Product.builder()
                .sku("SKU-100")
                .name("Old Product Name")
                .hsnSac("8471")
                .unit("PCS")
                .purchasePrice(new BigDecimal("100.00"))
                .sellingPrice(new BigDecimal("150.00"))
                .minStockLevel(new BigDecimal("5.00"))
                .active(true)
                .build();
        product.setId(id);

        com.antigravity.billing.dto.product.UpdateProductRequest updateReq = new com.antigravity.billing.dto.product.UpdateProductRequest();
        updateReq.setName("New Product Name");
        updateReq.setHsnSac("8471");
        updateReq.setUnit("PCS");
        updateReq.setPurchasePrice(new BigDecimal("120.00"));
        updateReq.setSellingPrice(new BigDecimal("180.00"));
        updateReq.setMinStockLevel(new BigDecimal("10.00"));

        when(productRepository.findById(id)).thenReturn(java.util.Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        UUID userId = UUID.randomUUID();
        ProductResponseDto result = productService.updateProduct(id, updateReq, userId, "admin");

        assertThat(result.getName()).isEqualTo("New Product Name");
        assertThat(result.getSellingPrice()).isEqualByComparingTo("180.00");
        verify(auditService, times(1)).logAction(eq(userId), eq("admin"), eq("UPDATE_PRODUCT"), any(), any(), any());
    }

    @Test
    @DisplayName("Successfully toggles product status")
    void testToggleProductStatus() {
        UUID id = UUID.randomUUID();
        Product product = Product.builder()
                .sku("SKU-200")
                .name("Active Item")
                .active(true)
                .build();
        product.setId(id);

        when(productRepository.findById(id)).thenReturn(java.util.Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        UUID userId = UUID.randomUUID();
        ProductResponseDto result = productService.toggleProductStatus(id, false, userId, "admin");

        assertThat(result.isActive()).isFalse();
        verify(auditService, times(1)).logAction(eq(userId), eq("admin"), eq("DEACTIVATE_PRODUCT"), any(), any(), any());
    }
}