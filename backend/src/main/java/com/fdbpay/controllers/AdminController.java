package com.fdbpay.controllers;

import com.fdbpay.schemas.response.ApiResponse;
import com.fdbpay.services.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final ReportingService reportingService;
    private final KycComplianceService kycComplianceService;
    private final ConfigService configService;

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboard() {
        return ResponseEntity.ok(ApiResponse.success(reportingService.getDashboardMetrics()));
    }

    @GetMapping("/kyc/pending")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPendingKyc(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(kycComplianceService.getPendingKycRequests(page, size)));
    }

    @PutMapping("/kyc/{userId}/review")
    public ResponseEntity<ApiResponse<Void>> reviewKyc(
            @PathVariable UUID userId,
            @RequestParam String status,
            @RequestParam(required = false) String notes,
            @RequestParam UUID reviewedBy) {
        kycComplianceService.reviewKyc(userId, status, notes, reviewedBy);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/config/fees")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getFeeSchedules() {
        return ResponseEntity.ok(ApiResponse.success(configService.getFeeSchedules()));
    }

    @GetMapping("/config/limits")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getLimits() {
        return ResponseEntity.ok(ApiResponse.success(configService.getTransactionLimits()));
    }

    @GetMapping("/config/system")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSystemParams() {
        return ResponseEntity.ok(ApiResponse.success(configService.getSystemParameters()));
    }
}
