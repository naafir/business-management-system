package com.antigravity.billing.controller;

import com.antigravity.billing.dto.common.ApiResponse;
import com.antigravity.billing.dto.report.*;
import com.antigravity.billing.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/gst-output")
    public ResponseEntity<ApiResponse<GstOutputSummaryDto>> getGstOutput(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return ResponseEntity.ok(ApiResponse.ok(reportService.getGstOutputSummary(startDate, endDate)));
    }

    @GetMapping("/gst-input")
    public ResponseEntity<ApiResponse<GstInputSummaryDto>> getGstInput(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return ResponseEntity.ok(ApiResponse.ok(reportService.getGstInputSummary(startDate, endDate)));
    }

    @GetMapping("/hsn-summary")
    public ResponseEntity<ApiResponse<List<HsnSummaryLineDto>>> getHsnSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return ResponseEntity.ok(ApiResponse.ok(reportService.getHsnSummary(startDate, endDate)));
    }

    @GetMapping("/monthly-trend")
    public ResponseEntity<ApiResponse<List<MonthlyTrendDto>>> getMonthlyTrend(
            @RequestParam(defaultValue = "0") int year
    ) {
        int targetYear = year > 0 ? year : LocalDate.now().getYear();
        return ResponseEntity.ok(ApiResponse.ok(reportService.getMonthlyTrend(targetYear)));
    }

    @GetMapping("/profit-loss")
    public ResponseEntity<ApiResponse<ProfitLossDto>> getProfitLoss(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return ResponseEntity.ok(ApiResponse.ok(reportService.getProfitLoss(startDate, endDate)));
    }
}
