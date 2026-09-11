package com.antigravity.billing.service;

import com.antigravity.billing.dto.gst.GstCalculationRequest;
import com.antigravity.billing.dto.gst.GstCalculationResult;
import com.antigravity.billing.dto.sale.CreateSaleRequest;
import com.antigravity.billing.dto.sale.SaleItemRequest;
import com.antigravity.billing.dto.sale.SaleResponseDto;
import com.antigravity.billing.entity.*;
import com.antigravity.billing.exception.ApiException;
import com.antigravity.billing.repository.BusinessSettingsRepository;
import com.antigravity.billing.repository.CustomerRepository;
import com.antigravity.billing.repository.ProductRepository;
import com.antigravity.billing.repository.SaleRepository;
import com.antigravity.billing.service.impl.SaleServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SaleServiceTest {

    @Mock
    private SaleRepository saleRepository;

    @Mock
    private CustomerRepository customerRepository;

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
    private SaleServiceImpl saleService;

    private Customer testCustomer;
    private Product testProduct;
    private GstRate testGstRate;
    private BusinessSettings testSettings;

    @BeforeEach
    void setUp() {
        testGstRate = GstRate.builder()
                .ratePercent(new BigDecimal("12.00"))
                .description("12% GST")
                .active(true)
                .build();
        testGstRate.setId(UUID.randomUUID());

        testCustomer = Customer.builder()
                .name("Taj Royal Bakers")
                .stateName("Maharashtra")
                .stateCode("27")
                .creditLimit(new BigDecimal("50000.00"))
                .outstandingBalance(BigDecimal.ZERO)
                .active(true)
                .build();
        testCustomer.setId(UUID.randomUUID());

        testProduct = Product.builder()
                .sku("ESS-VAN-01")
                .name("Natural Vanilla Flavor 500ml")
                .hsnSac("3302")
                .unit("PCS")
                .purchasePrice(new BigDecimal("400.00"))
                .sellingPrice(new BigDecimal("650.00"))
                .currentStock(new BigDecimal("10.00"))
                .minStockLevel(new BigDecimal("3.00"))
                .allowNegativeStock(false)
                .gstRate(testGstRate)
                .active(true)
                .build();
        testProduct.setId(UUID.randomUUID());

        testSettings = BusinessSettings.builder()
                .stateName("Maharashtra")
                .stateCode("27")
                .invoicePrefix("INV-2026-")
                .invoiceNextSeq(100L)
                .build();
        testSettings.setId(UUID.randomUUID());
    }

    @Test
    @DisplayName("Successfully creates sale invoice, deducts product stock, and updates customer balance")
    void testCreateSaleSuccess() {
        UUID userId = UUID.randomUUID();
        String username = "admin";

        SaleItemRequest itemReq = SaleItemRequest.builder()
                .productId(testProduct.getId())
                .quantity(new BigDecimal("3.00"))
                .unitPrice(new BigDecimal("650.00"))
                .discountPercent(BigDecimal.ZERO)
                .build();

        CreateSaleRequest request = CreateSaleRequest.builder()
                .customerId(testCustomer.getId())
                .saleDate(LocalDate.now())
                .items(List.of(itemReq))
                .amountPaid(new BigDecimal("1000.00"))
                .paymentMethod(PaymentMethod.UPI)
                .build();

        when(customerRepository.findById(testCustomer.getId())).thenReturn(Optional.of(testCustomer));
        when(businessSettingsRepository.findAll()).thenReturn(List.of(testSettings));
        when(productRepository.findByIdWithPessimisticLock(testProduct.getId())).thenReturn(Optional.of(testProduct));

        GstCalculationResult.GstLineItemResult lineRes = GstCalculationResult.GstLineItemResult.builder()
                .description(testProduct.getName())
                .quantity(new BigDecimal("3.00"))
                .unitPrice(new BigDecimal("650.00"))
                .grossAmount(new BigDecimal("1950.00"))
                .discount(BigDecimal.ZERO)
                .taxableValue(new BigDecimal("1950.00"))
                .gstRatePercent(new BigDecimal("12.00"))
                .cgstRatePercent(new BigDecimal("6.00"))
                .cgstAmount(new BigDecimal("117.00"))
                .sgstRatePercent(new BigDecimal("6.00"))
                .sgstAmount(new BigDecimal("117.00"))
                .igstRatePercent(BigDecimal.ZERO)
                .igstAmount(BigDecimal.ZERO)
                .lineTotal(new BigDecimal("2184.00"))
                .build();

        GstCalculationResult gstResult = GstCalculationResult.builder()
                .isInterState(false)
                .businessStateCode("27")
                .placeOfSupplyStateCode("27")
                .totalGross(new BigDecimal("1950.00"))
                .totalDiscount(BigDecimal.ZERO)
                .totalTaxableValue(new BigDecimal("1950.00"))
                .totalCgst(new BigDecimal("117.00"))
                .totalSgst(new BigDecimal("117.00"))
                .totalIgst(BigDecimal.ZERO)
                .totalTax(new BigDecimal("234.00"))
                .grandTotal(new BigDecimal("2184.00"))
                .items(List.of(lineRes))
                .build();

        when(gstCalculationService.calculate(any(GstCalculationRequest.class))).thenReturn(gstResult);
        when(saleRepository.existsBySaleNumber(anyString())).thenReturn(false);

        when(saleRepository.save(any(Sale.class))).thenAnswer(invocation -> {
            Sale s = invocation.getArgument(0);
            s.setId(UUID.randomUUID());
            return s;
        });

        SaleResponseDto result = saleService.createSale(request, userId, username);

        assertThat(result).isNotNull();
        assertThat(result.getSaleNumber()).isEqualTo("INV-2026-0100");
        assertThat(result.getGrandTotal()).isEqualByComparingTo("2184.00");
        assertThat(result.getAmountPaid()).isEqualByComparingTo("1000.00");
        assertThat(result.getBalanceDue()).isEqualByComparingTo("1184.00");
        assertThat(result.getPaymentStatus()).isEqualTo(PaymentStatus.PARTIALLY_PAID);

        // Verify product stock deducted by 3 (10 - 3 = 7)
        assertThat(testProduct.getCurrentStock()).isEqualByComparingTo("7.00");
        verify(productRepository).save(testProduct);

        // Verify inventory service recorded transaction
        verify(inventoryService).recordTransaction(
                eq(testProduct),
                eq(InventoryTransactionType.SALE),
                eq(new BigDecimal("3.00")),
                eq(new BigDecimal("10.00")),
                eq(new BigDecimal("7.00")),
                eq(new BigDecimal("400.00")), // Product COGS unit cost
                eq("SALE"),
                any(),
                eq("INV-2026-0100"),
                contains("Taj Royal Bakers"),
                eq(userId),
                eq(username)
        );

        // Verify customer outstanding balance increased by balance due (1184.00)
        assertThat(testCustomer.getOutstandingBalance()).isEqualByComparingTo("1184.00");
        verify(customerRepository).save(testCustomer);
    }

    @Test
    @DisplayName("Fails with BadRequest when stock is insufficient and allowNegativeStock is false")
    void testCreateSaleInsufficientStock() {
        SaleItemRequest itemReq = SaleItemRequest.builder()
                .productId(testProduct.getId())
                .quantity(new BigDecimal("15.00")) // Current stock is 10.00
                .build();

        CreateSaleRequest request = CreateSaleRequest.builder()
                .customerId(testCustomer.getId())
                .saleDate(LocalDate.now())
                .items(List.of(itemReq))
                .build();

        when(customerRepository.findById(testCustomer.getId())).thenReturn(Optional.of(testCustomer));
        when(businessSettingsRepository.findAll()).thenReturn(List.of(testSettings));
        when(productRepository.findByIdWithPessimisticLock(testProduct.getId())).thenReturn(Optional.of(testProduct));

        assertThatThrownBy(() -> saleService.createSale(request, UUID.randomUUID(), "admin"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Insufficient stock");
    }

    @Test
    @DisplayName("Fails when customer credit limit would be exceeded")
    void testCreateSaleCreditLimitExceeded() {
        testCustomer.setCreditLimit(new BigDecimal("1000.00"));
        testCustomer.setOutstandingBalance(new BigDecimal("900.00")); // Available credit = 100

        SaleItemRequest itemReq = SaleItemRequest.builder()
                .productId(testProduct.getId())
                .quantity(new BigDecimal("2.00"))
                .build();

        CreateSaleRequest request = CreateSaleRequest.builder()
                .customerId(testCustomer.getId())
                .saleDate(LocalDate.now())
                .items(List.of(itemReq))
                .amountPaid(BigDecimal.ZERO) // Balance due will be 1456
                .build();

        when(customerRepository.findById(testCustomer.getId())).thenReturn(Optional.of(testCustomer));
        when(businessSettingsRepository.findAll()).thenReturn(List.of(testSettings));
        when(productRepository.findByIdWithPessimisticLock(testProduct.getId())).thenReturn(Optional.of(testProduct));

        GstCalculationResult gstResult = GstCalculationResult.builder()
                .isInterState(false)
                .grandTotal(new BigDecimal("1456.00"))
                .totalGross(new BigDecimal("1300.00"))
                .totalTaxableValue(new BigDecimal("1300.00"))
                .totalTax(new BigDecimal("156.00"))
                .totalCgst(new BigDecimal("78.00"))
                .totalSgst(new BigDecimal("78.00"))
                .totalIgst(BigDecimal.ZERO)
                .totalDiscount(BigDecimal.ZERO)
                .items(List.of(GstCalculationResult.GstLineItemResult.builder()
                        .description(testProduct.getName())
                        .quantity(new BigDecimal("2.00"))
                        .unitPrice(new BigDecimal("650.00"))
                        .grossAmount(new BigDecimal("1300.00"))
                        .discount(BigDecimal.ZERO)
                        .taxableValue(new BigDecimal("1300.00"))
                        .gstRatePercent(new BigDecimal("12.00"))
                        .cgstAmount(new BigDecimal("78.00"))
                        .sgstAmount(new BigDecimal("78.00"))
                        .igstAmount(BigDecimal.ZERO)
                        .lineTotal(new BigDecimal("1456.00"))
                        .build()))
                .build();

        when(gstCalculationService.calculate(any(GstCalculationRequest.class))).thenReturn(gstResult);

        assertThatThrownBy(() -> saleService.createSale(request, UUID.randomUUID(), "admin"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Credit limit");
    }
}
