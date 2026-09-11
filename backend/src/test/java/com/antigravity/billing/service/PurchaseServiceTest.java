package com.antigravity.billing.service;

import com.antigravity.billing.dto.gst.GstCalculationRequest;
import com.antigravity.billing.dto.gst.GstCalculationResult;
import com.antigravity.billing.dto.purchase.CreatePurchaseRequest;
import com.antigravity.billing.dto.purchase.PurchaseItemRequest;
import com.antigravity.billing.dto.purchase.PurchaseResponseDto;
import com.antigravity.billing.entity.*;
import com.antigravity.billing.exception.ResourceNotFoundException;
import com.antigravity.billing.repository.BusinessSettingsRepository;
import com.antigravity.billing.repository.ProductRepository;
import com.antigravity.billing.repository.PurchaseRepository;
import com.antigravity.billing.repository.SupplierRepository;
import com.antigravity.billing.service.impl.PurchaseServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PurchaseServiceTest {

    @Mock
    private PurchaseRepository purchaseRepository;

    @Mock
    private SupplierRepository supplierRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private BusinessSettingsRepository businessSettingsRepository;

    @Mock
    private GstCalculationService gstCalculationService;

    @Mock
    private InventoryService inventoryService;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private PurchaseServiceImpl purchaseService;

    private Supplier testSupplier;
    private Product testProduct;
    private GstRate testGstRate;
    private BusinessSettings testSettings;

    @BeforeEach
    void setUp() {
        testGstRate = GstRate.builder()
                .ratePercent(new BigDecimal("18.00"))
                .description("18% GST")
                .active(true)
                .build();
        testGstRate.setId(UUID.randomUUID());

        testSupplier = Supplier.builder()
                .name("Alpha Spice Traders")
                .stateName("Maharashtra")
                .stateCode("27")
                .outstandingBalance(BigDecimal.ZERO)
                .active(true)
                .build();
        testSupplier.setId(UUID.randomUUID());

        testProduct = Product.builder()
                .sku("SPICE-CARD-01")
                .name("Green Cardamom 1kg")
                .hsnSac("0908")
                .unit("KG")
                .purchasePrice(new BigDecimal("1500.00"))
                .sellingPrice(new BigDecimal("2200.00"))
                .currentStock(new BigDecimal("10.00"))
                .minStockLevel(new BigDecimal("5.00"))
                .gstRate(testGstRate)
                .active(true)
                .build();
        testProduct.setId(UUID.randomUUID());

        testSettings = BusinessSettings.builder()
                .stateName("Maharashtra")
                .stateCode("27")
                .invoicePrefix("INV-2026-")
                .build();
        testSettings.setId(UUID.randomUUID());
    }

    @Test
    @DisplayName("Successfully creates purchase, increases product stock, and records inventory ledger")
    void testCreatePurchaseSuccess() {
        UUID userId = UUID.randomUUID();
        String username = "admin";

        PurchaseItemRequest itemReq = PurchaseItemRequest.builder()
                .productId(testProduct.getId())
                .quantity(new BigDecimal("5.00"))
                .unitPrice(new BigDecimal("1500.00"))
                .discountPercent(BigDecimal.ZERO)
                .build();

        CreatePurchaseRequest request = CreatePurchaseRequest.builder()
                .supplierId(testSupplier.getId())
                .supplierInvoiceNumber("SUP-INV-9988")
                .purchaseDate(LocalDate.now())
                .items(List.of(itemReq))
                .amountPaid(new BigDecimal("2000.00"))
                .paymentMethod(PaymentMethod.BANK_TRANSFER)
                .build();

        when(supplierRepository.findById(testSupplier.getId())).thenReturn(Optional.of(testSupplier));
        when(businessSettingsRepository.findAll()).thenReturn(List.of(testSettings));
        when(productRepository.findByIdWithPessimisticLock(testProduct.getId())).thenReturn(Optional.of(testProduct));

        GstCalculationResult.GstLineItemResult lineRes = GstCalculationResult.GstLineItemResult.builder()
                .description(testProduct.getName())
                .quantity(new BigDecimal("5.00"))
                .unitPrice(new BigDecimal("1500.00"))
                .grossAmount(new BigDecimal("7500.00"))
                .discount(BigDecimal.ZERO)
                .taxableValue(new BigDecimal("7500.00"))
                .gstRatePercent(new BigDecimal("18.00"))
                .cgstRatePercent(new BigDecimal("9.00"))
                .cgstAmount(new BigDecimal("675.00"))
                .sgstRatePercent(new BigDecimal("9.00"))
                .sgstAmount(new BigDecimal("675.00"))
                .igstRatePercent(BigDecimal.ZERO)
                .igstAmount(BigDecimal.ZERO)
                .lineTotal(new BigDecimal("8850.00"))
                .build();

        GstCalculationResult gstResult = GstCalculationResult.builder()
                .isInterState(false)
                .businessStateCode("27")
                .placeOfSupplyStateCode("27")
                .totalGross(new BigDecimal("7500.00"))
                .totalDiscount(BigDecimal.ZERO)
                .totalTaxableValue(new BigDecimal("7500.00"))
                .totalCgst(new BigDecimal("675.00"))
                .totalSgst(new BigDecimal("675.00"))
                .totalIgst(BigDecimal.ZERO)
                .totalTax(new BigDecimal("1350.00"))
                .grandTotal(new BigDecimal("8850.00"))
                .items(List.of(lineRes))
                .build();

        when(gstCalculationService.calculate(any(GstCalculationRequest.class))).thenReturn(gstResult);
        when(purchaseRepository.count()).thenReturn(0L);
        when(purchaseRepository.existsByPurchaseNumber(anyString())).thenReturn(false);

        when(purchaseRepository.save(any(Purchase.class))).thenAnswer(invocation -> {
            Purchase p = invocation.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });

        PurchaseResponseDto result = purchaseService.createPurchase(request, userId, username);

        assertThat(result).isNotNull();
        assertThat(result.getPurchaseNumber()).startsWith("PUR-");
        assertThat(result.getGrandTotal()).isEqualByComparingTo("8850.00");
        assertThat(result.getAmountPaid()).isEqualByComparingTo("2000.00");
        assertThat(result.getBalanceDue()).isEqualByComparingTo("6850.00");
        assertThat(result.getPaymentStatus()).isEqualTo(PaymentStatus.PARTIALLY_PAID);

        // Verify product stock increased by 5 (10 + 5 = 15)
        assertThat(testProduct.getCurrentStock()).isEqualByComparingTo("15.00");
        verify(productRepository).save(testProduct);

        // Verify inventory service recorded transaction
        verify(inventoryService).recordTransaction(
                eq(testProduct),
                eq(InventoryTransactionType.PURCHASE),
                eq(new BigDecimal("5.00")),
                eq(new BigDecimal("10.00")),
                eq(new BigDecimal("15.00")),
                eq(new BigDecimal("1500.00")),
                eq("PURCHASE"),
                any(),
                anyString(),
                contains("Alpha Spice Traders"),
                eq(userId),
                eq(username)
        );

        // Verify supplier outstanding balance increased by balance due (6850.00)
        assertThat(testSupplier.getOutstandingBalance()).isEqualByComparingTo("6850.00");
        verify(supplierRepository).save(testSupplier);
    }

    @Test
    @DisplayName("Throws ResourceNotFoundException when supplier does not exist")
    void testCreatePurchaseSupplierNotFound() {
        UUID fakeSupplierId = UUID.randomUUID();
        CreatePurchaseRequest request = CreatePurchaseRequest.builder()
                .supplierId(fakeSupplierId)
                .purchaseDate(LocalDate.now())
                .items(Collections.emptyList())
                .build();

        when(supplierRepository.findById(fakeSupplierId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> purchaseService.createPurchase(request, UUID.randomUUID(), "admin"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
