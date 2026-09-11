package com.antigravity.billing.dto.invoice;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceSummaryDto {
    private BigDecimal totalInvoicedAmount;
    private BigDecimal totalOutstandingReceivable;
    private long totalInvoicesCount;
    private long finalizedInvoicesCount;
    private long draftInvoicesCount;
}
