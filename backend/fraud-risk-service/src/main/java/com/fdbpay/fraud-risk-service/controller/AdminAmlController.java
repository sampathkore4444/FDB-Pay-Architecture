package com.fdbpay.fraud.risk.service.controller;

import com.fdbpay.fraud.risk.service.dto.request.AmlActionRequest;
import com.fdbpay.fraud.risk.service.dto.response.AdminAmlAlertResponse;
import com.fdbpay.fraud.risk.service.model.enums.AlertSeverity;
import com.fdbpay.fraud.risk.service.model.enums.AlertStatus;
import com.fdbpay.fraud.risk.service.service.FraudRiskService;
import com.fdbpay.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/admin/aml")
@RequiredArgsConstructor
public class AdminAmlController {

    private final FraudRiskService fraudRiskService;

    @GetMapping("/alerts")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAlerts(
            @RequestParam(required = false) AlertSeverity severity,
            @RequestParam(required = false) AlertStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<AdminAmlAlertResponse> alerts = fraudRiskService.getAmlAlerts(severity, status, page, size);
        return ResponseEntity.ok(ApiResponse.success(Map.of("alerts", alerts)));
    }

    @PostMapping("/{id}/action")
    public ResponseEntity<ApiResponse<AdminAmlAlertResponse>> actionAlert(
            @PathVariable UUID id,
            @Valid @RequestBody AmlActionRequest request) {
        return ResponseEntity.ok(ApiResponse.success(fraudRiskService.actionAlert(id, request.getAction(), request.getReason())));
    }
}
