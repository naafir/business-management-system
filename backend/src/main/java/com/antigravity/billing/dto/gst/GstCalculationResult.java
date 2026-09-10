package com.antigravity.billing.dto.gst;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GstCalculationResult {
    private boolean isInterState;
    private String businessStateCode;
    private String placeOfSupplyStateCode;
    private BigDecimal totalGross;
    private BigDecimal totalDiscount;
    private BigDecimal totalTaxableValue;
    private BigDecimal totalCgst;
    private BigDecimal totalSgst;
    private BigDecimal totalIgst;
    private BigDecimal totalTax;
    private BigDecimal grandTotal;
    private List<GstLineItemResult> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GstLineItemResult {
        private String description;
        private BigDecimal quantity;
        private BigDecimal unitPrice;
        private BigDecimal grossAmount;
        private BigDecimal discount;
        private BigDecimal taxableValue;
        private BigDecimal gstRatePercent;
        private BigDecimal cgstRatePercent;
        private BigDecimal cgstAmount;
        private BigDecimal sgstRatePercent;
        private BigDecimal sgstAmount;
        private BigDecimal igstRatePercent;
        private BigDecimal igstAmount;
        private BigDecimal lineTotal;
    }
}
