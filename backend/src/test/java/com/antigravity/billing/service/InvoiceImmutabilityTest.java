package com.antigravity.billing.service;

import com.antigravity.billing.dto.invoice.CreateInvoiceRequest;
import com.antigravity.billing.dto.invoice.InvoiceItemRequest;
import com.antigravity.billing.dto.invoice.InvoiceResponseDto;
import com.antigravity.billing.dto.payment.RecordPaymentRequest;
import com.antigravity.billing.entity.*;
import com.antigravity.billing.exception.ApiException;
import com.antigravity.billing.repository.BusinessSettingsRepository;
import com.antigravity.billing.repository.ProductRepository;
import com.antigravity.billing.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class InvoiceImmutabilityTest {

    @Autowired
    private InvoiceService invoiceService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private BusinessSettingsRepository businessSettingsRepository;

    @Autowired
    private UserRepository userRepository;

    private UUID productId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = userRepository.findAll().stream().findFirst().map(User::getId).orElse(null);

        BusinessSettings settings = businessSettingsRepository.findAll().stream().findFirst().orElse(null);
        if (settings == null) {
            settings = BusinessSettings.builder()
                    .legalName("Original Enterprise Ltd")
                    .tradeName("Original Brand")
                    .gstin("27AAAAA0000A1Z5")
                    .stateName("Maharashtra")
                    .stateCode("27")
                    .bankName("State Bank of India")
                    .bankAccountNumber("1234567890")
                    .invoicePrefix("INV-")
                    .invoiceNextSeq(500L)
                    .defaultCurrency("INR")
                    .build();
            businessSettingsRepository.save(settings);
        } else {
            settings.setLegalName("Original Enterprise Ltd");
            settings.setTradeName("Original Brand");
            settings.setBankName("State Bank of India");
            businessSettingsRepository.save(settings);
        }

        Product product = Product.builder()
                .name("Test Immutability Widget")
                .sku("IMMUT-WIDGET-" + UUID.randomUUID().toString().substring(0, 6))
                .hsnSac("21039090")
                .unit("PCS")
                .sellingPrice(new BigDecimal("100.00"))
                .currentStock(new BigDecimal("500"))
                .allowNegativeStock(true)
                .active(true)
                .build();
        Product saved = productRepository.save(product);
        productId = saved.getId();
    }

    @Test
    @DisplayName("Historical seller details snapshot on invoice remain unchanged after current BusinessSettings are modified")
    void testHistoricalSellerSnapshotImmutability() {
        CreateInvoiceRequest request = CreateInvoiceRequest.builder()
                .customerName("Test Snapshot Client")
                .placeOfSupplyState("Maharashtra")
                .placeOfSupplyCode("27")
                .invoiceDate(LocalDate.now())
                .items(List.of(
                        InvoiceItemRequest.builder()
                                .productId(productId)
                                .quantity(new BigDecimal("5"))
                                .unitPrice(new BigDecimal("100.00"))
                                .build()
                ))
                .build();

        InvoiceResponseDto created = invoiceService.createInvoice(request, userId, "admin");
        assertThat(created.getSellerLegalName()).isEqualTo("Original Enterprise Ltd");
        assertThat(created.getSellerTradeName()).isEqualTo("Original Brand");
        assertThat(created.getSellerBankName()).isEqualTo("State Bank of India");

        // Mutate current live business settings
        BusinessSettings liveSettings = businessSettingsRepository.findAll().get(0);
        liveSettings.setLegalName("Renamed Future Enterprise Ltd");
        liveSettings.setTradeName("Renamed Future Brand");
        liveSettings.setBankName("HDFC Bank");
        businessSettingsRepository.save(liveSettings);

        // Fetch invoice again and verify historical snapshot remains completely intact
        InvoiceResponseDto retrieved = invoiceService.getInvoiceById(created.getId());
        assertThat(retrieved.getSellerLegalName()).isEqualTo("Original Enterprise Ltd");
        assertThat(retrieved.getSellerTradeName()).isEqualTo("Original Brand");
        assertThat(retrieved.getSellerBankName()).isEqualTo("State Bank of India");
    }

    @Test
    @DisplayName("Cancelled invoice cannot be finalized and rejects payment operations")
    void testCancelledInvoiceRules() {
        CreateInvoiceRequest request = CreateInvoiceRequest.builder()
                .customerName("Cancel Client")
                .placeOfSupplyState("Maharashtra")
                .placeOfSupplyCode("27")
                .invoiceDate(LocalDate.now())
                .items(List.of(
                        InvoiceItemRequest.builder()
                                .productId(productId)
                                .quantity(new BigDecimal("1"))
                                .unitPrice(new BigDecimal("100.00"))
                                .build()
                ))
                .build();

        InvoiceResponseDto created = invoiceService.createInvoice(request, userId, "admin");
        InvoiceResponseDto cancelled = invoiceService.cancelInvoice(created.getId(), userId, "admin");

        assertThat(cancelled.getStatus()).isEqualTo(InvoiceStatus.CANCELLED);

        // Attempting to finalize a cancelled invoice throws ApiException
        assertThatThrownBy(() -> invoiceService.finalizeInvoice(created.getId()))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Cannot finalize a cancelled invoice");

        // Attempting to record payment against cancelled invoice throws ApiException
        RecordPaymentRequest payReq = RecordPaymentRequest.builder()
                .amountPaid(new BigDecimal("50.00"))
                .paymentMethod(PaymentMethod.CASH)
                .build();

        assertThatThrownBy(() -> paymentService.recordInvoicePayment(created.getId(), payReq, userId, "admin"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Cannot record payment for a cancelled invoice");
    }
}
