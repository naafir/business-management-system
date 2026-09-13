package com.antigravity.billing.dto.report;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class MonthlyTrendDto {
    private int year;
    private int month;
    private String monthName;
    private BigDecimal salesAmount;
    private BigDecimal purchasesAmount;
    private BigDecimal expensesAmount;
    private long salesCount;
    private long purchasesCount;
}
