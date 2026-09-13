package com.antigravity.billing.dto.report;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class GstOutputSummaryDto {
    private BigDecimal totalRevenue;
    private BigDecimal totalTaxableValue;
    private BigDecimal totalCgstCollected;
    private BigDecimal totalSgstCollected;
    private BigDecimal totalIgstCollected;
    private BigDecimal totalTaxCollected;
    private BigDecimal b2bTaxableValue;
    private BigDecimal b2cTaxableValue;
    private long totalInvoicesCount;
}
