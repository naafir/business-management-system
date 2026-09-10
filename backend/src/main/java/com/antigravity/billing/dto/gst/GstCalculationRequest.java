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
public class GstCalculationRequest {
    private String businessStateCode;
    private String placeOfSupplyStateCode;
    private List<GstLineItemInput> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GstLineItemInput {
        private String description;
        private BigDecimal quantity;
        private BigDecimal unitPrice;
        @Builder.Default
        private BigDecimal discount = BigDecimal.ZERO;
        private BigDecimal gstRatePercent;
    }
}
