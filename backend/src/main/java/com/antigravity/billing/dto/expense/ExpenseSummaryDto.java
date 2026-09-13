package com.antigravity.billing.dto.expense;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ExpenseSummaryDto {
    private BigDecimal totalExpensesAmount;
    private BigDecimal eligibleGstInputAmount;
    private long totalExpensesCount;
}
