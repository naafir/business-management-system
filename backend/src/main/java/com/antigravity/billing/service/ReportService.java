package com.antigravity.billing.service;

import com.antigravity.billing.dto.report.*;

import java.time.LocalDate;
import java.util.List;

public interface ReportService {
    GstOutputSummaryDto getGstOutputSummary(LocalDate startDate, LocalDate endDate);
    GstInputSummaryDto getGstInputSummary(LocalDate startDate, LocalDate endDate);
    List<HsnSummaryLineDto> getHsnSummary(LocalDate startDate, LocalDate endDate);
    List<MonthlyTrendDto> getMonthlyTrend(int year);
    ProfitLossDto getProfitLoss(LocalDate startDate, LocalDate endDate);
}
