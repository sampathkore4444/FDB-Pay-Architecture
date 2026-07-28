package com.fdbpay.controllers;

import com.fdbpay.schemas.request.BulkDisbursementRequest;
import com.fdbpay.schemas.response.ApiResponse;
import com.fdbpay.services.CorporateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/corp")
@RequiredArgsConstructor
public class CorporateController {

    private final CorporateService corporateService;

    @PostMapping("/bulk-disburse")
    public ResponseEntity<ApiResponse<Map<String, Object>>> initiateBulkDisbursement(
            @RequestParam UUID corporateUserId,
            @Valid @RequestBody BulkDisbursementRequest request) {
        Map<String, Object> response = corporateService.initiateBulkDisbursement(corporateUserId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/bulk-disburse/{batchId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getBulkDisbursementStatus(
            @RequestParam UUID corporateUserId,
            @PathVariable UUID batchId) {
        Map<String, Object> response = corporateService.getBulkDisbursementStatus(corporateUserId, batchId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/reconciliation")
    public ResponseEntity<byte[]> downloadReconciliation(
            @RequestParam UUID corporateUserId,
            @RequestParam String period) {
        byte[] file = corporateService.downloadReconciliation(corporateUserId, period);
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=reconciliation.csv")
                .body(file);
    }

    @PostMapping("/payroll/schedule")
    public ResponseEntity<ApiResponse<Map<String, Object>>> schedulePayroll(
            @RequestParam UUID corporateUserId,
            @RequestBody Map<String, Object> payrollRequest) {
        Map<String, Object> response = corporateService.schedulePayroll(corporateUserId, payrollRequest);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
