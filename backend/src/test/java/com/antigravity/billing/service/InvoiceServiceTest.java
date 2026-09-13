package com.antigravity.billing.service;

import com.antigravity.billing.dto.gst.GstCalculationRequest;
import com.antigravity.billing.dto.gst.GstCalculationResult;
import com.antigravity.billing.dto.invoice.CreateInvoiceRequest;
import com.antigravity.billing.dto.invoice.InvoiceItemRequest;
import com.antigravity.billing.dto.invoice.InvoiceResponseDto;
import com.antigravity.billing.entity.*;
import com.antigravity.billing.repository.*;
import com.antigravity.billing.service.impl.InvoiceServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private SaleRepository saleRepository;
    @Mock
    private BusinessSettingsRepository businessSettingsRepository;
    @Mock
    private GstCalculationService gstCalculationService;
    @Mock
    private PdfGeneratorService pdfGeneratorService;

    @InjectMocks
    private InvoiceServiceImpl invoiceService;

    private Product testProduct;
    private Customer testCustomer;
    private BusinessSettings testSettings;
    private UUID productId;
    private UUID customerId;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();
        GstRate gstRateObj = GstRate.builder().ratePercent(BigDecimal.valueOf(18.00)).build();
        testProduct = Product.builder()
                .name("Premium Sauce Base")
                .sku("SAUCE-001")
                .hsnSac("21039090")
                .unit("PCS")
                .sellingPrice(BigDecimal.valueOf(250.00))
                .gstRate(gstRateObj)
                .build();
        ReflectionTestUtils.setField(testProduct, "id", productId);

        customerId = UUID.randomUUID();
        testCustomer = Customer.builder()
                .name("Acme Hotel Pvt Ltd")
                .gstin("27AAACA123411Z5")
                .stateCode("27")
                .build();
        ReflectionTestUtils.setField(testCustomer, "id", customerId);

        testSettings = BusinessSettings.builder()
                .legalName("Antigravity Foods")
                .stateCode("27")
                .invoicePrefix("INV-")
                .invoiceNextSeq(1L)
                .build();
        ReflectionTestUtils.setField(testSettings, "id", UUID.randomUUID());
    }

    @Test
    @DisplayName("Create Invoice - Should calculate totals and save invoice successfully")
    void createInvoice_Success() {
        when(businessSettingsRepository.findAll()).thenReturn(List.of(testSettings));
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(testCustomer));
        when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));

        GstCalculationResult.GstLineItemResult itemRes = GstCalculationResult.GstLineItemResult.builder()
                .quantity(BigDecimal.valueOf(1))
                .taxableValue(BigDecimal.valueOf(250.00))
                .gstRatePercent(BigDecimal.valueOf(18.00))
                .cgstAmount(BigDecimal.valueOf(22.50))
                .sgstAmount(BigDecimal.valueOf(22.50))
                .igstAmount(BigDecimal.ZERO)
                .lineTotal(BigDecimal.valueOf(295.00))
                .build();

        GstCalculationResult gstResult = GstCalculationResult.builder()
                .totalGross(BigDecimal.valueOf(250.00))
                .totalDiscount(BigDecimal.ZERO)
                .totalTaxableValue(BigDecimal.valueOf(250.00))
                .totalCgst(BigDecimal.valueOf(22.50))
                .totalSgst(BigDecimal.valueOf(22.50))
                .totalIgst(BigDecimal.ZERO)
                .totalTax(BigDecimal.valueOf(45.00))
                .grandTotal(BigDecimal.valueOf(295.00))
                .items(List.of(itemRes))
                .build();

        when(gstCalculationService.calculate(any(GstCalculationRequest.class))).thenReturn(gstResult);

        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> {
            Invoice i = inv.getArgument(0);
            ReflectionTestUtils.setField(i, "id", UUID.randomUUID());
            return i;
        });

        CreateInvoiceRequest request = CreateInvoiceRequest.builder()
                .customerId(customerId)
                .invoiceDate(LocalDate.now())
                .items(List.of(
                        InvoiceItemRequest.builder()
                                .productId(productId)
                                .quantity(BigDecimal.valueOf(1))
                                .unitPrice(BigDecimal.valueOf(250.00))
                                .discountPercent(BigDecimal.ZERO)
                                .build()
                ))
                .build();

        InvoiceResponseDto response = invoiceService.createInvoice(request, UUID.randomUUID(), "admin");

        assertThat(response).isNotNull();
        assertThat(response.getInvoiceNumber()).startsWith("INV-");
        assertThat(response.getCustomerName()).isEqualTo("Acme Hotel Pvt Ltd");
        assertThat(response.getGrandTotal()).isEqualByComparingTo(BigDecimal.valueOf(295.00));
        verify(invoiceRepository).save(any(Invoice.class));
    }

    @Test
    @DisplayName("Finalize Invoice - Should update status to FINALIZED")
    void finalizeInvoice_Success() {
        UUID invId = UUID.randomUUID();
        Invoice invoice = Invoice.builder()
                .invoiceNumber("INV-2026-00001")
                .status(InvoiceStatus.DRAFT)
                .build();
        ReflectionTestUtils.setField(invoice, "id", invId);

        when(invoiceRepository.findById(invId)).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        InvoiceResponseDto response = invoiceService.finalizeInvoice(invId);

        assertThat(response.getStatus()).isEqualTo(InvoiceStatus.FINALIZED);
        verify(invoiceRepository).save(invoice);
    }
}
