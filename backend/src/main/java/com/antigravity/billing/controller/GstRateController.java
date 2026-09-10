package com.antigravity.billing.controller;

import com.antigravity.billing.dto.common.ApiResponse;
import com.antigravity.billing.dto.gst.GstCalculationRequest;
import com.antigravity.billing.dto.gst.GstCalculationResult;
import com.antigravity.billing.entity.GstRate;
import com.antigravity.billing.repository.GstRateRepository;
import com.antigravity.billing.service.GstCalculationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/gst")
@RequiredArgsConstructor
public class GstRateController {

    private final GstRateRepository gstRateRepository;
    private final GstCalculationService gstCalculationService;

    @GetMapping("/rates")
    public ResponseEntity<ApiResponse<List<GstRate>>> getActiveRates() {
        List<GstRate> rates = gstRateRepository.findByActiveTrueOrderByRatePercentAsc();
        return ResponseEntity.ok(ApiResponse.ok(rates));
    }

    @PostMapping("/calculate")
    public ResponseEntity<ApiResponse<GstCalculationResult>> previewCalculation(
            @Valid @RequestBody GstCalculationRequest request) {
        GstCalculationResult result = gstCalculationService.calculate(request);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
