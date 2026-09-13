package com.antigravity.billing.dto.report;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class GstInputSummaryDto {
    private BigDecimal totalPurchaseValue;
    private BigDecimal totalTaxableValue;
    private BigDecimal totalCgstPaid;
    private BigDecimal totalSgstPaid;
    private BigDecimal totalIgstPaid;
    private BigDecimal totalInputTaxCredit;
    private long totalPurchasesCount;
}
