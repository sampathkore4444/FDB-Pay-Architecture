package com.fdbpay.corporate.controller;

import com.fdbpay.corporate.dto.request.BulkDisbursementRequest;
import com.fdbpay.corporate.dto.request.PayrollScheduleRequest;
import com.fdbpay.corporate.service.CorporateService;
import com.fdbpay.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/corp")
@RequiredArgsConstructor
public class CorporateController {

    private final CorporateService corporateService;

    @PostMapping("/bulk-disburse")
    public ResponseEntity<ApiResponse<?>> initiateBulkDisbursement(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody BulkDisbursementRequest request) {
        var response = corporateService.initiateBulkDisbursement(userId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/bulk-disburse/{batchId}")
    public ResponseEntity<ApiResponse<?>> getBulkDisbursementStatus(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID batchId) {
        var response = corporateService.getStatus(userId, batchId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/reconciliation")
    public ResponseEntity<ApiResponse<?>> downloadReconciliation(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestParam String period) {
        var response = corporateService.downloadReconciliation(userId, period);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/payroll/schedule")
    public ResponseEntity<ApiResponse<?>> schedulePayroll(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody PayrollScheduleRequest request) {
        var response = corporateService.schedulePayroll(userId, request);
        return ResponseEntity.ok(response);
    }
}
