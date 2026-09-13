package com.antigravity.billing.dto.report;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ProfitLossDto {
    private BigDecimal totalRevenue;
    private BigDecimal totalCogs;        // Cost of goods sold (from inventory purchase price)
    private BigDecimal grossProfit;
    private BigDecimal grossMarginPercent;
    private BigDecimal totalExpenses;
    private BigDecimal netProfit;
    private BigDecimal totalTaxCollected;
    private BigDecimal totalInputTaxCredit;
    private BigDecimal netTaxLiability;
}
