package com.antigravity.billing.service;

import com.antigravity.billing.dto.gst.GstCalculationRequest;
import com.antigravity.billing.dto.gst.GstCalculationResult;
import com.antigravity.billing.exception.ApiException;
import com.antigravity.billing.service.impl.GstCalculationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GstCalculationServiceTest {

    private GstCalculationService gstService;

    @BeforeEach
    void setUp() {
        gstService = new GstCalculationServiceImpl();
    }

    @Test
    @DisplayName("Intra-state transaction: Splits GST into 50% CGST and 50% SGST")
    void testIntraStateGstSplit() {
        // Business state: 27 (Maharashtra), Place of supply: 27 (Maharashtra)
        GstCalculationRequest request = GstCalculationRequest.builder()
                .businessStateCode("27")
                .placeOfSupplyStateCode("27")
                .items(List.of(
                        GstCalculationRequest.GstLineItemInput.builder()
                                .description("Laptop")
                                .quantity(new BigDecimal("2"))
                                .unitPrice(new BigDecimal("50000.00"))
                                .discount(new BigDecimal("5000.00")) // Gross = 100,000, Taxable = 95,000
                                .gstRatePercent(new BigDecimal("18.00"))
                                .build()
                ))
                .build();

        GstCalculationResult result = gstService.calculate(request);

        assertThat(result.isInterState()).isFalse();
        assertThat(result.getTotalGross()).isEqualByComparingTo("100000.00");
        assertThat(result.getTotalDiscount()).isEqualByComparingTo("5000.00");
        assertThat(result.getTotalTaxableValue()).isEqualByComparingTo("95000.00");

        // 18% on 95,000 is 17,100 -> CGST: 8,550 (9%), SGST: 8,550 (9%), IGST: 0
        assertThat(result.getTotalCgst()).isEqualByComparingTo("8550.00");
        assertThat(result.getTotalSgst()).isEqualByComparingTo("8550.00");
        assertThat(result.getTotalIgst()).isEqualByComparingTo("0.00");
        assertThat(result.getTotalTax()).isEqualByComparingTo("17100.00");
        assertThat(result.getGrandTotal()).isEqualByComparingTo("112100.00");
    }

    @Test
    @DisplayName("Inter-state transaction: Applies 100% IGST, zero CGST/SGST")
    void testInterStateIgst() {
        // Business state: 27 (Maharashtra), Place of supply: 29 (Karnataka)
        GstCalculationRequest request = GstCalculationRequest.builder()
                .businessStateCode("27")
                .placeOfSupplyStateCode("29")
                .items(List.of(
                        GstCalculationRequest.GstLineItemInput.builder()
                                .description("Server Hardware")
                                .quantity(new BigDecimal("1"))
                                .unitPrice(new BigDecimal("200000.00"))
                                .discount(BigDecimal.ZERO)
                                .gstRatePercent(new BigDecimal("18.00"))
                                .build()
                ))
                .build();

        GstCalculationResult result = gstService.calculate(request);

        assertThat(result.isInterState()).isTrue();
        assertThat(result.getTotalCgst()).isEqualByComparingTo("0.00");
        assertThat(result.getTotalSgst()).isEqualByComparingTo("0.00");
        assertThat(result.getTotalIgst()).isEqualByComparingTo("36000.00");
        assertThat(result.getGrandTotal()).isEqualByComparingTo("236000.00");
    }

    @Test
    @DisplayName("Exempt / 0% GST calculation")
    void testZeroGst() {
        GstCalculationRequest request = GstCalculationRequest.builder()
                .businessStateCode("27")
                .placeOfSupplyStateCode("27")
                .items(List.of(
                        GstCalculationRequest.GstLineItemInput.builder()
                                .description("Fresh Grains")
                                .quantity(new BigDecimal("10"))
                                .unitPrice(new BigDecimal("100.00"))
                                .discount(BigDecimal.ZERO)
                                .gstRatePercent(BigDecimal.ZERO)
                                .build()
                ))
                .build();

        GstCalculationResult result = gstService.calculate(request);

        assertThat(result.getTotalTaxableValue()).isEqualByComparingTo("1000.00");
        assertThat(result.getTotalTax()).isEqualByComparingTo("0.00");
        assertThat(result.getGrandTotal()).isEqualByComparingTo("1000.00");
    }

    @Test
    @DisplayName("Multiple items with different GST slabs (5%, 12%, 28%)")
    void testMultipleSlabs() {
        GstCalculationRequest request = GstCalculationRequest.builder()
                .businessStateCode("27")
                .placeOfSupplyStateCode("27")
                .items(List.of(
                        GstCalculationRequest.GstLineItemInput.builder()
                                .description("Item A (5%)")
                                .quantity(new BigDecimal("1"))
                                .unitPrice(new BigDecimal("100.00"))
                                .discount(BigDecimal.ZERO)
                                .gstRatePercent(new BigDecimal("5.00"))
                                .build(),
                        GstCalculationRequest.GstLineItemInput.builder()
                                .description("Item B (12%)")
                                .quantity(new BigDecimal("1"))
                                .unitPrice(new BigDecimal("200.00"))
                                .discount(BigDecimal.ZERO)
                                .gstRatePercent(new BigDecimal("12.00"))
                                .build(),
                        GstCalculationRequest.GstLineItemInput.builder()
                                .description("Item C (28%)")
                                .quantity(new BigDecimal("1"))
                                .unitPrice(new BigDecimal("300.00"))
                                .discount(BigDecimal.ZERO)
                                .gstRatePercent(new BigDecimal("28.00"))
                                .build()
                ))
                .build();

        GstCalculationResult result = gstService.calculate(request);

        // Taxable: 100 + 200 + 300 = 600
        // Item A tax (5%): CGST 2.50, SGST 2.50
        // Item B tax (12%): CGST 12.00, SGST 12.00
        // Item C tax (28%): CGST 42.00, SGST 42.00
        // Total CGST: 56.50, Total SGST: 56.50 -> Total Tax: 113.00
        // Grand Total: 713.00
        assertThat(result.getTotalTaxableValue()).isEqualByComparingTo("600.00");
        assertThat(result.getTotalCgst()).isEqualByComparingTo("56.50");
        assertThat(result.getTotalSgst()).isEqualByComparingTo("56.50");
        assertThat(result.getTotalTax()).isEqualByComparingTo("113.00");
        assertThat(result.getGrandTotal()).isEqualByComparingTo("713.00");
    }

    @Test
    @DisplayName("Rejects discount greater than gross amount")
    void testDiscountExceedingGross() {
        GstCalculationRequest request = GstCalculationRequest.builder()
                .businessStateCode("27")
                .placeOfSupplyStateCode("27")
                .items(List.of(
                        GstCalculationRequest.GstLineItemInput.builder()
                                .description("Invalid Discount Item")
                                .quantity(new BigDecimal("1"))
                                .unitPrice(new BigDecimal("100.00"))
                                .discount(new BigDecimal("150.00"))
                                .gstRatePercent(new BigDecimal("18.00"))
                                .build()
                ))
                .build();

        assertThatThrownBy(() -> gstService.calculate(request))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Discount cannot exceed line item gross total");
    }
}
