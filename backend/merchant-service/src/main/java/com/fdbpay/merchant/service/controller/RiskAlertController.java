package com.fdbpay.merchant.service.controller;

import com.fdbpay.merchant.service.dto.response.RiskAlertResponse;
import com.fdbpay.merchant.service.service.RiskAlertService;
import com.fdbpay.shared.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/merchant/risk-alerts")
@RequiredArgsConstructor
public class RiskAlertController {

    private final RiskAlertService riskAlertService;
    private final MerchantAccessHelper accessHelper;

    @GetMapping
    public ApiResponse<List<RiskAlertResponse>> getAlerts(
            @RequestParam UUID userId,
            @RequestParam UUID walletId) {
        return ApiResponse.success(riskAlertService.getAlerts(accessHelper.resolveMerchantId(userId), walletId));
    }

    @PutMapping("/{alertId}/acknowledge")
    public ApiResponse<RiskAlertResponse> acknowledge(
            @RequestParam UUID userId,
            @PathVariable UUID alertId) {
        return ApiResponse.success(riskAlertService.acknowledge(accessHelper.resolveMerchantId(userId), alertId));
    }
}
