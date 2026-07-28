package com.fdbpay.agent.controller;

import com.fdbpay.agent.dto.request.WithdrawCommissionRequest;
import com.fdbpay.agent.service.CommissionService;
import com.fdbpay.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/agent/commission")
@RequiredArgsConstructor
public class CommissionController {

    private final CommissionService commissionService;

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<?>> getCommissionHistory(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var response = commissionService.getCommissionHistory(userId, page, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<?>> getCommissionSummary(
            @RequestHeader("X-User-Id") UUID userId) {
        var response = commissionService.getSummary(userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/withdraw")
    public ResponseEntity<ApiResponse<?>> withdrawCommission(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody WithdrawCommissionRequest request) {
        var response = commissionService.withdrawCommission(userId, request);
        return ResponseEntity.ok(response);
    }
}
