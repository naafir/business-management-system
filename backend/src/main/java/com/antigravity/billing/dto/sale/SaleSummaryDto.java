package com.antigravity.billing.dto.sale;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaleSummaryDto {
    private BigDecimal totalSalesAmount;
    private BigDecimal totalOutstandingReceivable;
    private long totalSalesCount;
    private long pendingSalesCount;
}
