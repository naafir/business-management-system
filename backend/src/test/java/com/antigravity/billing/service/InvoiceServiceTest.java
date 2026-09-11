package com.antigravity.billing.service;

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

    @BeforeEach
    void setUp() {
        testProduct = Product.builder()
                .id(UUID.randomUUID())
                .name("Premium Sauce Base")
                .sku("SAUCE-001")
                .hsnSac("21039090")
                .unit("PCS")
                .sellingPrice(BigDecimal.valueOf(250.00))
                .gstRatePercent(BigDecimal.valueOf(18.00))
                .build();

        testCustomer = Customer.builder()
                .id(UUID.randomUUID())
                .name("Acme Hotel Pvt Ltd")
                .gstin("27AAACA123411Z5")
                .stateCode("27")
                .build();

        testSettings = BusinessSettings.builder()
                .id(UUID.randomUUID())
                .legalName("Antigravity Foods")
                .stateCode("27")
                .invoicePrefix("INV-")
                .invoiceNextSeq(1L)
                .build();
    }

    @Test
    @DisplayName("Create Invoice - Should calculate totals and save invoice successfully")
    void createInvoice_Success() {
        when(businessSettingsRepository.findTopByOrderByIdAsc()).thenReturn(Optional.of(testSettings));
        when(customerRepository.findById(testCustomer.getId())).thenReturn(Optional.of(testCustomer));
        when(productRepository.findById(testProduct.getId())).thenReturn(Optional.of(testProduct));

        GstCalculationService.GstBreakdown gst = new GstCalculationService.GstBreakdown(
                BigDecimal.valueOf(22.50), BigDecimal.valueOf(22.50), BigDecimal.ZERO, BigDecimal.valueOf(45.00)
        );
        when(gstCalculationService.calculateGst(any(), any(), eq(true))).thenReturn(gst);

        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> {
            Invoice i = inv.getArgument(0);
            i.setId(UUID.randomUUID());
            return i;
        });

        CreateInvoiceRequest request = CreateInvoiceRequest.builder()
                .customerId(testCustomer.getId())
                .invoiceDate(LocalDate.now())
                .items(List.of(
                        InvoiceItemRequest.builder()
                                .productId(testProduct.getId())
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
                .id(invId)
                .invoiceNumber("INV-2026-00001")
                .status(InvoiceStatus.DRAFT)
                .build();

        when(invoiceRepository.findById(invId)).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        InvoiceResponseDto response = invoiceService.finalizeInvoice(invId);

        assertThat(response.getStatus()).isEqualTo(InvoiceStatus.FINALIZED);
        verify(invoiceRepository).save(invoice);
    }
}
