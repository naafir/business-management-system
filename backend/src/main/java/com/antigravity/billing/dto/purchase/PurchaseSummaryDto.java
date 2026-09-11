package com.antigravity.billing.dto.purchase;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseSummaryDto {
    private BigDecimal totalPurchasesAmount;
    private BigDecimal totalOutstandingPayable;
    private long totalPurchasesCount;
    private long pendingPurchasesCount;
}
