package com.antigravity.billing.controller;

import com.antigravity.billing.dto.common.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, String>>> checkHealth() {
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "status", "UP",
                "service", "Billing Backend",
                "version", "1.0.0"
        )));
    }
}
