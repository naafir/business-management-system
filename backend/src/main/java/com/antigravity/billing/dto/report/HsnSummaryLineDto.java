package com.antigravity.billing.dto.report;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class HsnSummaryLineDto {
    private String hsnSac;
    private BigDecimal totalQuantity;
    private BigDecimal totalTaxableValue;
    private BigDecimal totalCgst;
    private BigDecimal totalSgst;
    private BigDecimal totalIgst;
    private BigDecimal totalTax;
    private BigDecimal totalAmount;
}
