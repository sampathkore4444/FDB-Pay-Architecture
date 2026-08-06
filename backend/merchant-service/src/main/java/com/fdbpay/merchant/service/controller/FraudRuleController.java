package com.fdbpay.merchant.service.controller;

import com.fdbpay.merchant.service.dto.request.FraudRuleRequest;
import com.fdbpay.merchant.service.dto.response.FraudRuleResponse;
import com.fdbpay.merchant.service.service.FraudRuleService;
import com.fdbpay.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/merchant/fraud-rules")
@RequiredArgsConstructor
public class FraudRuleController {

    private final FraudRuleService fraudRuleService;
    private final MerchantAccessHelper accessHelper;

    @GetMapping
    public ApiResponse<List<FraudRuleResponse>> list(@RequestParam UUID userId) {
        return ApiResponse.success(fraudRuleService.listRules(accessHelper.resolveMerchantId(userId)));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<FraudRuleResponse> create(@RequestParam UUID userId, @Valid @RequestBody FraudRuleRequest request) {
        return ApiResponse.success(fraudRuleService.createRule(accessHelper.resolveMerchantId(userId), request));
    }

    @PutMapping("/{ruleId}/toggle")
    public ApiResponse<FraudRuleResponse> toggle(@RequestParam UUID userId, @PathVariable UUID ruleId) {
        return ApiResponse.success(fraudRuleService.toggleRule(accessHelper.resolveMerchantId(userId), ruleId));
    }

    @DeleteMapping("/{ruleId}")
    public ApiResponse<Void> delete(@RequestParam UUID userId, @PathVariable UUID ruleId) {
        fraudRuleService.deleteRule(accessHelper.resolveMerchantId(userId), ruleId);
        return ApiResponse.success(null);
    }
}
