package com.fdbpay.fraud.risk.service.controller;

import com.fdbpay.fraud.risk.service.dto.request.SanctionScreeningRequest;
import com.fdbpay.fraud.risk.service.dto.request.TransactionEvaluationRequest;
import com.fdbpay.fraud.risk.service.dto.response.FraudAlertResponse;
import com.fdbpay.fraud.risk.service.dto.response.FraudEvaluationResponse;
import com.fdbpay.fraud.risk.service.model.enums.AlertStatus;
import com.fdbpay.fraud.risk.service.service.FraudRiskService;
import com.fdbpay.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/fraud")
@RequiredArgsConstructor
@Tag(name = "Fraud Risk", description = "Fraud detection and risk management endpoints")
public class FraudRiskController {

    private final FraudRiskService fraudRiskService;

    @GetMapping("/rules")
    @Operation(summary = "Get fraud detection rules")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getRules() {
        return ResponseEntity.ok(ApiResponse.success(List.of(
                Map.of("id", "high-amount", "name", "High Amount Threshold", "threshold", 20_000_000, "severity", "HIGH"),
                Map.of("id", "velocity-check", "name", "Transaction Velocity", "maxPerMinute", 10, "severity", "MEDIUM"),
                Map.of("id", "self-transfer", "name", "Self-Transfer Detection", "enabled", true, "severity", "HIGH"),
                Map.of("id", "sanction-screening", "name", "Sanctions Screening", "enabled", true, "severity", "CRITICAL")
        )));
    }

    @GetMapping("/transactions")
    @Operation(summary = "Get flagged transactions")
    public ResponseEntity<ApiResponse<Page<FraudAlertResponse>>> getFlaggedTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(fraudRiskService.getAlerts(page, size)));
    }

    @PostMapping("/evaluate")
    @Operation(summary = "Evaluate a transaction for fraud risk")
    public ResponseEntity<ApiResponse<FraudEvaluationResponse>> evaluateTransaction(
            @Valid @RequestBody TransactionEvaluationRequest request) {
        FraudEvaluationResponse response = fraudRiskService.evaluateTransaction(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/screen-sanctions")
    @Operation(summary = "Screen a person against sanctions lists")
    public ResponseEntity<ApiResponse<Boolean>> screenSanctions(
            @Valid @RequestBody SanctionScreeningRequest request) {
        boolean matched = fraudRiskService.screenSanctions(request);
        return ResponseEntity.ok(ApiResponse.success(matched));
    }

    @GetMapping("/alerts")
    @Operation(summary = "Get paginated fraud alerts")
    public ResponseEntity<ApiResponse<Page<FraudAlertResponse>>> getAlerts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<FraudAlertResponse> alerts = fraudRiskService.getAlerts(page, size);
        return ResponseEntity.ok(ApiResponse.success(alerts));
    }

    @PutMapping("/alerts/{id}/resolve")
    @Operation(summary = "Resolve a fraud alert")
    public ResponseEntity<ApiResponse<FraudAlertResponse>> resolveAlert(
            @PathVariable java.util.UUID id,
            @RequestParam AlertStatus status) {
        FraudAlertResponse response = fraudRiskService.resolveAlert(id, status);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
