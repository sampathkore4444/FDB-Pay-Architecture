package com.fdbpay.corporate.controller;

import com.fdbpay.corporate.dto.request.CreatePayrollRunRequest;
import com.fdbpay.corporate.service.PayrollService;
import com.fdbpay.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/corp/payroll")
@RequiredArgsConstructor
public class PayrollController {

    private final PayrollService payrollService;

    @PostMapping("/create")
    public ResponseEntity<ApiResponse<?>> createPayrollRun(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody CreatePayrollRunRequest request) {
        var response = payrollService.createPayrollRun(userId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<ApiResponse<?>> submitPayroll(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID userId) {
        var response = payrollService.submitPayroll(id, userId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<?>> approvePayroll(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID approverId) {
        var response = payrollService.approvePayroll(id, approverId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> getPayrollRun(@PathVariable UUID id) {
        var response = payrollService.getPayrollRun(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<?>> getPayrollHistory(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var response = payrollService.getPayrollHistory(userId, PageRequest.of(page, size));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/employees")
    public ResponseEntity<ApiResponse<?>> getPayrollEmployees(@PathVariable UUID id) {
        var response = payrollService.getPayrollEmployees(id);
        return ResponseEntity.ok(response);
    }
}
